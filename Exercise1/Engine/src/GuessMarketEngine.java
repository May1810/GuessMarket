import dto.BuyResultDTO;
import dto.EventDTO;
import dto.OptionDTO;
import dto.TradeDTO;
import models.Event;
import models.Option;
import models.Trade;
import xml.GMEvent;
import xml.GuessMarketDescriptor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuessMarketEngine implements IGuessMarketEngine {

    private List<Event> events;

    public GuessMarketEngine() {
        this.events = new ArrayList<>();
    }

    private Event findLogicEventById(int eventId) {
        for (Event event : this.events) {
            if (event.getId() == eventId) {
                return event;
            }
        }
        throw new IllegalArgumentException("Event with ID " + eventId + " was not found.");
    }

    private Option findOptionByName(Event event, String optionName) {
        for (Option opt : event.getOptions()) {
            if (opt.getOptionName().equals(optionName)) {
                return opt;
            }
        }
        return null;
    }

    @Override
    public void loadXmlFile(String filePath) {
        if (filePath == null || !filePath.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("Error: the file needs to end with .xml");
        }

        File xmlFile = new File(filePath);
        if (!xmlFile.exists()) {
            throw new IllegalArgumentException("The file was not found at the given path.");
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarketDescriptor.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            GuessMarketDescriptor descriptor = (GuessMarketDescriptor) unmarshaller.unmarshal(xmlFile);
            if (descriptor.getEvents() == null || descriptor.getEvents().isEmpty()) {
                throw new IllegalArgumentException("The XML file contains no events.");
            }

            List<Event> tempLogicEvents = new ArrayList<>();
            Set<Integer> seenIds = new HashSet<>();

            for (GMEvent xmlEvent : descriptor.getEvents()) {
                if (!seenIds.add(xmlEvent.getId())) {
                    throw new IllegalArgumentException("Event with duplicate ID found: " + xmlEvent.getId());
                }
                if (xmlEvent.getCommission().getValue() < 0 || xmlEvent.getCommission().getValue() > 90) {
                    throw new IllegalArgumentException(
                            "Commission must be between 0 and 90. Found: " + xmlEvent.getCommission().getValue());
                }
                if (xmlEvent.getOptions() == null || xmlEvent.getOptions().size() != 2) {
                    throw new IllegalArgumentException(
                            "Each event must have exactly 2 options. Event ID: " + xmlEvent.getId());
                }

                List<Option> eventOptions = new ArrayList<>();
                for (String optionName : xmlEvent.getOptions()) {
                    eventOptions.add(new Option(optionName));
                }

                Event newLogicEvent = new Event(
                        xmlEvent.getId(),
                        xmlEvent.getName(),
                        xmlEvent.getCommission().getValue(),
                        xmlEvent.getDescription(),
                        xmlEvent.getCommission().getType(),
                        xmlEvent.getMethod().getLmsr().getB(),
                        eventOptions
                );
                tempLogicEvents.add(newLogicEvent);
            }
            this.events = tempLogicEvents;

        } catch (JAXBException e) {
            throw new IllegalArgumentException("Error in the file data-XML: " + e.getMessage());
        }
    }

    @Override
    public boolean hasLoadedData() {
        return events != null && !events.isEmpty();
    }

    @Override
    public List<EventDTO> getAllEvents() {
        List<EventDTO> dtoList = new ArrayList<>();
        for (Event logicEvent : this.events) {
            dtoList.add(toEventDTO(logicEvent));
        }
        return dtoList;
    }

    @Override
    public List<EventDTO> getActiveEvents() {
        List<EventDTO> dtoList = new ArrayList<>();
        for (Event logicEvent : this.events) {
            if (logicEvent.isActive()) {
                dtoList.add(toEventDTO(logicEvent));
            }
        }
        return dtoList;
    }

    @Override
    public EventDTO getEventById(int id) {
        return toEventDTO(findLogicEventById(id));
    }

    @Override
    public BuyResultDTO buyShares(int eventId, String optionName, int amount) {
        Event currentEvent = findLogicEventById(eventId);
        if (!currentEvent.isActive()) {
            throw new IllegalArgumentException("Event with ID " + eventId + " is already closed for trading.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Share amount must be a positive number.");
        }

        Option selectedOption = findOptionByName(currentEvent, optionName);
        if (selectedOption == null) {
            throw new IllegalArgumentException("Option '" + optionName + "' does not exist in event " + eventId);
        }

        double oldCost = calculateMarketCost(currentEvent, optionName, 0);
        double newCost = calculateMarketCost(currentEvent, optionName, amount);
        double basePrice = newCost - oldCost;
        double commission = calculateCommissionAmount(currentEvent, basePrice, "on-purchase");

        selectedOption.setShareBought(selectedOption.getShareBought() + amount);
        currentEvent.getAccount().addInvestment(basePrice);
        if (commission > 0) {
            currentEvent.getAccount().addCommission(commission);
        }
        currentEvent.addTrade(new Trade(optionName, amount, basePrice));

        return new BuyResultDTO(basePrice, commission);
    }

    @Override
    public void closeEvent(int eventId, String winningOptionName) {
        Event currentEvent = findLogicEventById(eventId);
        if (!currentEvent.isActive()) {
            throw new IllegalArgumentException("Event with ID " + eventId + " is already closed for trading.");
        }

        Option winningOption = findOptionByName(currentEvent, winningOptionName);
        if (winningOption == null) {
            throw new IllegalArgumentException(
                    "Error: The winning option '" + winningOptionName + "' does not exist in this event.");
        }

        int winningShares = winningOption.getShareBought();
        currentEvent.setActive(false);
        currentEvent.setWinningOptionName(winningOptionName);

        // on-close commission is taken from the winners' payout ($1 per winning share)
        double payout = winningShares * 1.0;
        double closeCommission = calculateCommissionAmount(currentEvent, payout, "on-close");
        if (closeCommission > 0) {
            currentEvent.getAccount().addCommission(closeCommission);
        }

        currentEvent.getAccount().deductFunds(payout);
    }

    private double calculateCommissionAmount(Event event, double baseAmount, String currentAction) {
        if (currentAction.equals(event.getCommissionType())) {
            double commissionPercent = event.getCommission() / 100.0;
            return baseAmount * commissionPercent;
        }
        return 0.0;
    }

    private double calculateMarketCost(Event event, String targetOptionName, int additionalShares) {
        double sum = 0.0;
        double b = event.getB();

        for (Option opt : event.getOptions()) {
            int currentShares = opt.getShareBought();
            if (opt.getOptionName().equals(targetOptionName)) {
                currentShares += additionalShares;
            }
            sum += Math.exp(currentShares / (double) b);
        }
        return b * Math.log(sum);
    }

    private double calculateOptionProbability(Event event, Option targetOption) {
        double b = event.getB();
        double sum = 0.0;

        for (Option opt : event.getOptions()) {
            sum += Math.exp(opt.getShareBought() / (double) b);
        }

        return Math.exp(targetOption.getShareBought() / (double) b) / sum;
    }

    private EventDTO toEventDTO(Event event) {
        List<OptionDTO> optionDTOs = new ArrayList<>();
        for (Option opt : event.getOptions()) {
            optionDTOs.add(new OptionDTO(
                    opt.getOptionName(),
                    opt.getShareBought(),
                    calculateOptionProbability(event, opt)
            ));
        }

        List<TradeDTO> tradeDTOs = new ArrayList<>();
        for (Trade trade : event.getTradeHistory()) {
            tradeDTOs.add(new TradeDTO(
                    trade.getOptionName(),
                    trade.getQuantity(),
                    trade.getPricePaid()
            ));
        }

        return new EventDTO(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.isActive(),
                optionDTOs,
                event.getCommission(),
                event.getCommissionType(),
                event.getAccount().getBalance(),
                event.getAccount().getTotalCommissionCollected(),
                tradeDTOs,
                event.getWinningOptionName()
        );
    }
}
