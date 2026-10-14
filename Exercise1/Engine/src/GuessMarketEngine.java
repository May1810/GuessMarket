import dto.BuyResultDTO;
import dto.EventDTO;
import dto.OptionDTO;
import dto.OrderDTO;
import dto.ParticipantDTO;
import dto.TradeDTO;
import dto.UserDTO;
import dto.UserEventDTO;
import models.BookOrder;
import models.Event;
import models.EventStatus;
import models.EventType;
import models.Option;
import models.OptionBook;
import models.Trade;
import models.User;
import models.UserPosition;
import xml.GMEvent;
import xml.GuessMarketDescriptor;
import xml.GMUser;
import xml.GMMarketMakerEvent;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuessMarketEngine implements IGuessMarketEngine {

    private int nextEventId = 1;
    private List<Event> events;
    private List<User> users;
    private long orderSeq;

    public GuessMarketEngine() {
        this.events = new ArrayList<>();
        this.users = new ArrayList<>();
    }

    private static double r2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public void registerUser(String name) {
        if (findUserOrNull(name) != null) {
            return;
        }
        // אם הוא חדש, מוסיפים אותו לרשימת המשתמשים עם 0 כסף
        users.add(new User(name.trim(), 0));
    }

    public void addCash(String userName, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }
        findUser(userName).credit(amount, "Top up");
    }

    public User findUserOrNull(String name) {
        String trimmed = name.trim();
        for (User u : this.users) {
            if (u.getName().equalsIgnoreCase(trimmed)) {
                return u;
            }
        }
        return null; // לא מצאנו? מחזירים null בשקט
    }

    private Event findLogicEventById(int eventId) {
        for (Event event : this.events) {
            if (event.getId() == eventId) {
                return event;
            }
        }
        throw new IllegalArgumentException("Event with ID " + eventId + " was not found.");
    }

    private Event findEventByNameOrNull(String name) {
        String trimmed = name.trim();
        for (Event e : this.events) {
            if (e.getName().equalsIgnoreCase(trimmed)) {
                return e; // מצאנו אירוע עם אותו שם!
            }
        }
        return null; // לא מצאנו, השם הזה פנוי
    }

    private Option findOptionByName(Event event, String optionName) {
        if (optionName == null) {
            return null;
        }
        for (Option opt : event.getOptions()) {
            if (opt.getOptionName().equalsIgnoreCase(optionName.trim())) {
                return opt;
            }
        }
        return null;
    }

    private User findUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name is required.");
        }
        String wanted = name.trim();
        User u = findUserOrNull(wanted);
        if(u != null){
            return u;
        }
        throw new IllegalArgumentException("User '" + wanted + "' was not found.");
    }

    private boolean hasUsers() {
        return users != null && !users.isEmpty();
    }

    private User findMarketMaker(Event event) {
        if (event.getMarketMakerName() != null) {
            return findUser(event.getMarketMakerName());
        }
        for (User user : users) {
            if (user.isMarketMakerOf(event.getId())) {
                return user;
            }
        }
        throw new IllegalArgumentException("Event " + event.getId() + " has no market maker.");
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

            boolean ex2 = descriptor.getUsers() != null && !descriptor.getUsers().isEmpty();
            validateLoadedDescriptor(descriptor, ex2);

            List<Event> tempEvents = new ArrayList<>();
            List<User> tempUsers = new ArrayList<>();
            Map<Integer, String> mmByEvent = new HashMap<>();

            if (ex2) {
                for (GMUser xmlUser : descriptor.getUsers()) {
                    String name = xmlUser.getName().trim();
                    User user = new User(name, xmlUser.getInitialCash());
                    if (xmlUser.getMarketMakerEvents() != null) {
                        for (GMMarketMakerEvent mm : xmlUser.getMarketMakerEvents()) {
                            user.getMmEventIds().add(mm.getId());
                            mmByEvent.put(mm.getId(), name);
                        }
                    }
                    tempUsers.add(user);
                }
            }

            for (GMEvent xmlEvent : descriptor.getEvents()) {
                List<Option> options = new ArrayList<>();
                for (String optionName : xmlEvent.getOptions()) {
                    options.add(new Option(optionName.trim()));
                }
                Event logicEvent;
                if (xmlEvent.getMethod().getLmsr() != null) {
                    logicEvent = new Event(
                            xmlEvent.getId(),
                            xmlEvent.getName(),
                            xmlEvent.getCommission().getValue(),
                            xmlEvent.getDescription(),
                            xmlEvent.getCommission().getType(),
                            xmlEvent.getMethod().getLmsr().getB(),
                            options,
                            !ex2
                    );
                } else {
                    xml.GMOrderBook ob = xmlEvent.getMethod().getOrderBook();
                    logicEvent = new Event(
                            xmlEvent.getId(),
                            xmlEvent.getName(),
                            options,
                            ob.isAllowMint(),
                            ob.getInitial(),
                            ob.getD(),
                            xmlEvent.getCommission().getValue(),
                            xmlEvent.getDescription(),
                            xmlEvent.getCommission().getType()
                    );
                }
                if (ex2) {
                    logicEvent.setMarketMakerName(mmByEvent.get(xmlEvent.getId()));
                }
                tempEvents.add(logicEvent);
            }
            this.events = tempEvents;
            this.users = tempUsers;
            this.orderSeq = 0;
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Error in the file data-XML: " + e.getMessage());
        }
    }

    public int addEventsFromXml(InputStream xml, String uploaderName) {
        // שלב 0: מוודאים שמי שהעלה את הקובץ קיים במנוע (יזרוק שגיאה אם לא)
        User uploader = findUser(uploaderName);

        GuessMarketDescriptor descriptor;
        // שלב 1: פותחים את ה-XML מתוך הזרם של הרשת (InputStream) ולא מקובץ שמור
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarketDescriptor.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            descriptor = (GuessMarketDescriptor) unmarshaller.unmarshal(xml);
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Error in the file data-XML: " + e.getMessage());
        }

        // שלב 2: חוסמים קבצים של תרגיל 2 (שיש בהם תגית משתמשים) או קבצים ללא אירועים
        if (descriptor.getUsers() != null && !descriptor.getUsers().isEmpty()) {
            throw new IllegalArgumentException("Schema 3 files must not contain users.");
        }
        if (descriptor.getEvents() == null || descriptor.getEvents().isEmpty()) {
            throw new IllegalArgumentException("The XML file contains no events.");
        }

        // שלב 3: מכינים כלים לבדיקות ("הכל או כלום")
        int tempId = this.nextEventId; // לוקחים את המספר הבא הפנוי
        List<Event> tempEvents = new ArrayList<>(); // רשימה זמנית
        Set<String> namesInFile = new HashSet<>();  // למניעת כפילויות בקובץ

        // השתמשתי פה ב-getEvents() בדיוק כמו בקוד המקורי שלך
        for (GMEvent xmlEvent : descriptor.getEvents()) {

            // בדיקת שם (חובה)
            if (xmlEvent.getName() == null || xmlEvent.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Event name is required.");
            }
            String trimmedName = xmlEvent.getName().trim();

            // חסימת כפילות בתוך הקובץ עצמו
            if (!namesInFile.add(trimmedName.toLowerCase())) {
                throw new IllegalArgumentException("Duplicate event name in file: " + trimmedName);
            }

            // חסימת כפילות מול אירועים שכבר קיימים בשרת
            if (findEventByNameOrNull(trimmedName) != null) {
                throw new IllegalArgumentException("Event name already exists on server: " + trimmedName);
            }

            // בדיקות תקינות (מתרגיל 1)
            int commission = xmlEvent.getCommission().getValue();
            if (commission < 0 || commission > 90) {
                throw new IllegalArgumentException("Commission must be between 0 and 90.");
            }

            // קריאת האפשרויות בדיוק כמו אצלך בקוד הישן
            List<Option> options = new ArrayList<>();
            for (String optionName : xmlEvent.getOptions()) {
                options.add(new Option(optionName.trim()));
            }

            if (options.size() < 2) {
                throw new IllegalArgumentException("Event must have at least 2 options.");
            }

            // יצירת האירוע הלוגי - השתמשתי בבנאים המדויקים שלך!
            Event logicEvent;
            if (xmlEvent.getMethod().getLmsr() != null) {
                if (xmlEvent.getMethod().getOrderBook() != null) {
                    throw new IllegalArgumentException("Event must have exactly one method (LMSR or OrderBook).");
                }
                int b = xmlEvent.getMethod().getLmsr().getB();
                if (b <= 0) throw new IllegalArgumentException("LMSR 'b' value must be > 0.");

                logicEvent = new Event(
                        tempId, // המונה החדש שלנו במקום xmlEvent.getId()
                        trimmedName,
                        commission,
                        xmlEvent.getDescription(),
                        xmlEvent.getCommission().getType(),
                        b,
                        options,
                        false // חשוב: מעבירים false כדי שייווצר כסגור (Not started) ולא נצטרך ex2
                );
            } else {
                // העתקתי את זה בדיוק כמו אצלך עם הקידומת xml.GMOrderBook
                xml.GMOrderBook ob = xmlEvent.getMethod().getOrderBook();
                if (ob == null) throw new IllegalArgumentException("Event must have a method.");
                if (ob.getD() <= 0 || ob.getInitial() < 0) {
                    throw new IllegalArgumentException("OrderBook 'd' must be > 0 and 'initial' >= 0.");
                }

                logicEvent = new Event(
                        tempId, // המונה החדש שלנו במקום xmlEvent.getId()
                        trimmedName,
                        options,
                        ob.isAllowMint(),
                        ob.getInitial(),
                        ob.getD(),
                        commission,
                        xmlEvent.getDescription(),
                        xmlEvent.getCommission().getType()
                );
            }

            // האירוע מושלם! מוסיפים אותו לרשימה הזמנית ומקדמים את ה-ID
            tempEvents.add(logicEvent);
            tempId++;
        }

        // שלב 5: אם הגענו עד לכאן (אף שגיאה לא קפצה) - הקובץ מושלם לגמרי!
        // עכשיו באמת משנים את השרת:
        this.events.addAll(tempEvents); // מוסיפים ולא דורסים (this.events = tempEvents)!
        this.nextEventId = tempId; // מעדכנים את המונה האמיתי

        for (Event added : tempEvents) {
            added.setMarketMakerName(uploader.getName());
            // שומרים את האירוע תחת המשתמש שהעלה אותו
            uploader.getMmEventIds().add(added.getId());
        }

        // מחזירים לסרבלט את כמות האירועים שהתווספו
        return tempEvents.size();
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
    public List<UserDTO> getAllUsers() {
        List<UserDTO> result = new ArrayList<>();
        for (User user : users) {
            result.add(toUserDTO(user));
        }
        return result;
    }

    @Override
    public UserDTO getUserByName(String name) {
        return toUserDTO(findUser(name));
    }

    @Override
    public BuyResultDTO buyShares(int eventId, String optionName, int amount) {
        if (hasUsers()) {
            throw new IllegalArgumentException("Select a user in the Users tab and buy as that user.");
        }
        return buySharesAnonymous(eventId, optionName, amount);
    }

    @Override
    public void closeEvent(int eventId, String winningOptionName) {
        if (hasUsers()) {
            throw new IllegalArgumentException("Only the market maker can close an event from the Users tab.");
        }
        closeEventAnonymous(eventId, winningOptionName);
    }

    @Override
    public BuyResultDTO buyShares(String userName, int eventId, String optionName, int amount) {
        User user = findUser(userName);
        user.requireNotBlocked();
        Event currentEvent = findLogicEventById(eventId);
        if (!currentEvent.isLmsr()) {
            throw new IllegalArgumentException("Use Place Order for Order Book events.");
        }
        requireActive(currentEvent);
        if (amount <= 0) {
            throw new IllegalArgumentException("Share amount must be a positive number.");
        }
        Option selectedOption = findOptionByName(currentEvent, optionName);
        if (selectedOption == null) {
            throw new IllegalArgumentException("Option '" + optionName + "' does not exist in event " + eventId);
        }

        double oldCost = calculateMarketCost(currentEvent, selectedOption.getOptionName(), 0);
        double newCost = calculateMarketCost(currentEvent, selectedOption.getOptionName(), amount);
        double basePrice = r2(newCost - oldCost);
        double commission = r2(calculateCommissionAmount(currentEvent, basePrice, "on-purchase"));
        double total = r2(basePrice + commission);
        user.requireFunds(total);

        user.debit(total, "LMSR buy");
        selectedOption.setShareBought(selectedOption.getShareBought() + amount);
        currentEvent.getAccount().addInvestment(basePrice);
        payCommissionToMm(currentEvent, commission);
        UserPosition pos = user.position(eventId);
        pos.addShares(selectedOption.getOptionName(), amount);
        pos.addPaid(basePrice);
        pos.addCommission(commission);
        currentEvent.addTrade(new Trade(user.getName(), selectedOption.getOptionName(), amount, basePrice, commission));
        return blockedMessage(user, new BuyResultDTO(basePrice, commission));
    }

    @Override
    public void openEvent(String userName, int eventId) {
        User user = findUser(userName);
        user.requireNotBlocked();
        Event event = findLogicEventById(eventId);
        if (event.getStatus() != EventStatus.NOT_STARTED) {
            throw new IllegalArgumentException("Event " + eventId + " cannot be opened (status: " + statusText(event) + ").");
        }
        if (!user.isMarketMakerOf(eventId)) {
            throw new IllegalArgumentException("Only the market maker (" + event.getMarketMakerName() + ") can open this event.");
        }

        if (event.isLmsr()) {
            double subsidy = r2(event.subsidyAmount());
            user.requireFunds(subsidy);
            user.debit(subsidy, "Opened LMSR event");
            event.getAccount().addInvestment(subsidy);
        } else {
            double cost = event.getInitialInvestment();
            user.requireFunds(cost);
            user.debit(cost, "Opened order book");
            event.getAccount().addInvestment(cost);
            int pairs = event.getD() <= 0 ? 0 : (int) Math.round(event.getInitialInvestment() / event.getD());
            UserPosition pos = user.position(eventId);
            for (Option option : event.getOptions()) {
                option.setShareBought(option.getShareBought() + pairs);
                pos.addShares(option.getOptionName(), pairs);
            }
            pos.addPaid(cost);
        }
        event.setStatus(EventStatus.ACTIVE);
    }

    @Override
    public void closeEvent(String userName, int eventId, String winningOptionName) {
        User mm = findUser(userName);
        mm.requireNotBlocked();
        Event event = findLogicEventById(eventId);
        if (!mm.isMarketMakerOf(eventId)) {
            throw new IllegalArgumentException("Only the market maker (" + event.getMarketMakerName() + ") can close this event.");
        }
        requireActive(event);
        Option winningOption = findOptionByName(event, winningOptionName);
        if (winningOption == null) {
            throw new IllegalArgumentException(
                    "Error: The winning option '" + winningOptionName + "' does not exist in this event.");
        }

        cancelAllOpenOrders(event);

        double payoutUnit = event.isLmsr() ? 1.0 : event.getD();
        for (User user : users) {
            if (!user.hasParticipated(eventId)) {
                continue;
            }
            UserPosition pos = user.position(eventId);
            int winningShares = pos.getShares(winningOption.getOptionName());
            double gross = r2(winningShares * payoutUnit);
            double closeCommission = r2(calculateCommissionAmount(event, gross, "on-close"));
            double net = r2(gross - closeCommission);
            if (gross > 0) {
                event.getAccount().deductFunds(gross);
                user.credit(net, "Winning payout");
                payCommissionToMm(event, closeCommission);
                pos.addCommission(closeCommission);
            }
            pos.setClosedPnl(r2(net - pos.getTotalPaid()));
        }

        double leftover = event.getAccount().getBalance();
        if (leftover > 0) {
            event.getAccount().deductFunds(leftover);
            mm.credit(leftover, "Leftover event pot");
        } else if (leftover < 0) {
            mm.debit(-leftover, "Covered event shortfall");
            event.getAccount().setBalance(0);
        }

        event.setStatus(EventStatus.CLOSED);
        event.setWinningOptionName(winningOption.getOptionName());
    }

    @Override
    public BuyResultDTO placeOrder(String userName, int eventId, String optionName, String side, int quantity, double price) {
        User user = findUser(userName);
        user.requireNotBlocked();
        Event event = findLogicEventById(eventId);
        if (event.isLmsr()) {
            throw new IllegalArgumentException("Use Buy for LMSR events.");
        }
        requireActive(event);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive number.");
        }
        Option option = findOptionByName(event, optionName);
        if (option == null) {
            throw new IllegalArgumentException("Option '" + optionName + "' does not exist in event " + eventId);
        }
        double maxPrice = r2(event.getD() - 0.01);
        double orderPrice = r2(price);
        if (orderPrice <= 0 || orderPrice > maxPrice + 1e-9) {
            throw new IllegalArgumentException(
                    "Price must be greater than 0 and at most " + String.format("%.2f", maxPrice)
                            + " (d - 0.01).");
        }
        boolean isBuy = "buy".equalsIgnoreCase(side.trim());
        boolean isSell = "sell".equalsIgnoreCase(side.trim());
        if (!isBuy && !isSell) {
            throw new IllegalArgumentException("Side must be Buy or Sell.");
        }

        OptionBook book = event.bookOf(option.getOptionName());
        BookOrder incoming;
        if (isBuy) {
            double reserve = r2(quantity * orderPrice);
            user.reserveCash(reserve);
            incoming = new BookOrder(++orderSeq, user.getName(), true, quantity, orderPrice, reserve);
            matchIncomingBuy(event, book, incoming, option);
            tryMint(event, incoming, option);
            if (incoming.getRemaining() > 0) {
                double keep = r2(incoming.getRemaining() * incoming.getPrice());
                double refund = r2(incoming.getReservedCash() - keep);
                user.releaseCash(refund);
                incoming.setReservedCash(keep);
                book.add(incoming);
            } else if (incoming.getReservedCash() > 0) {
                user.releaseCash(incoming.getReservedCash());
                incoming.setReservedCash(0);
            }
        } else {
            UserPosition pos = user.position(eventId);
            if (pos.getAvailableShares(option.getOptionName()) < quantity) {
                throw new IllegalArgumentException(
                        "User '" + user.getName() + "' does not have enough '" + option.getOptionName() + "' shares to sell.");
            }
            pos.addReservedShares(option.getOptionName(), quantity);
            incoming = new BookOrder(++orderSeq, user.getName(), false, quantity, orderPrice, 0);
            matchIncomingSell(event, book, incoming, option);
            if (incoming.getRemaining() > 0) {
                book.add(incoming);
            } else {
                pos.removeReservedShares(option.getOptionName(), 0);
            }
        }
        user.position(eventId);
        return blockedMessage(user, new BuyResultDTO(0, 0, "Order processed."));
    }

    private void matchIncomingBuy(Event event, OptionBook book, BookOrder buy, Option option) {
        while (buy.getRemaining() > 0) {
            BookOrder ask = firstAskAtOrBelow(book, buy);
            if (ask == null) {
                break;
            }
            int qty = Math.min(buy.getRemaining(), ask.getRemaining());
            executeTrade(event, option, buy, ask, qty, ask.getPrice());
        }
    }

    private void matchIncomingSell(Event event, OptionBook book, BookOrder sell, Option option) {
        while (sell.getRemaining() > 0) {
            BookOrder bid = firstBidAtOrAbove(book, sell);
            if (bid == null) {
                break;
            }
            int qty = Math.min(sell.getRemaining(), bid.getRemaining());
            executeTrade(event, option, bid, sell, qty, bid.getPrice());
        }
    }

    private BookOrder firstAskAtOrBelow(OptionBook book, BookOrder buy) {
        for (BookOrder ask : book.getAsks()) {
            if (ask.getPrice() <= buy.getPrice() + 1e-9
                    && !ask.getUserName().equalsIgnoreCase(buy.getUserName())) {
                return ask;
            }
        }
        return null;
    }

    private BookOrder firstBidAtOrAbove(OptionBook book, BookOrder sell) {
        for (BookOrder bid : book.getBids()) {
            if (bid.getPrice() + 1e-9 >= sell.getPrice()
                    && !bid.getUserName().equalsIgnoreCase(sell.getUserName())) {
                return bid;
            }
        }
        return null;
    }

    private void executeTrade(Event event, Option option, BookOrder buy, BookOrder sell, int qty, double tradePrice) {
        User buyer = findUser(buy.getUserName());
        User seller = findUser(sell.getUserName());
        double cost = r2(qty * tradePrice);
        double limitReserved = r2(qty * buy.getPrice());
        buyer.consumeReserved(cost);
        buy.setReservedCash(r2(buy.getReservedCash() - limitReserved));
        buyer.releaseCash(r2(limitReserved - cost));
        if (buy.getReservedCash() < 0) {
            buy.setReservedCash(0);
        }

        seller.credit(cost, "Sold shares");
        UserPosition sellerPos = seller.position(event.getId());
        sellerPos.removeReservedShares(option.getOptionName(), qty);
        sellerPos.removeShares(option.getOptionName(), qty);

        UserPosition buyerPos = buyer.position(event.getId());
        buyerPos.addShares(option.getOptionName(), qty);
        buyerPos.addPaid(cost);

        double commission = r2(calculateCommissionAmount(event, cost, "on-purchase"));
        if (commission > 0) {
            buyer.requireFunds(commission);
            buyer.debit(commission, "Purchase commission");
            payCommissionToMm(event, commission);
            buyerPos.addCommission(commission);
        }

        buy.setRemaining(buy.getRemaining() - qty);
        sell.setRemaining(sell.getRemaining() - qty);
        if (buy.getRemaining() == 0 && !buy.isBuy()) {
            // no-op
        }
        if (sell.getRemaining() == 0) {
            event.bookOf(option.getOptionName()).remove(sell);
        }
        if (buy.getRemaining() == 0) {
            event.bookOf(option.getOptionName()).remove(buy);
        }
        event.bookOf(option.getOptionName()).setLastPrice(tradePrice);
        event.addTrade(new Trade(buyer.getName(), option.getOptionName(), qty, cost, commission));
    }

    private void tryMint(Event event, BookOrder incoming, Option incomingOption) {
        if (!event.isAllowMint() || incoming.getRemaining() <= 0 || !incoming.isBuy()) {
            return;
        }
        boolean minted;
        do {
            minted = false;
            Option other = otherOption(event, incomingOption);
            OptionBook otherBook = event.bookOf(other.getOptionName());
            BookOrder waiting = otherBook.bestBid(incoming.getUserName());
            if (waiting == null) {
                break;
            }
            if (incoming.getPrice() + waiting.getPrice() + 1e-9 < event.getD()) {
                break;
            }
            int qty = Math.min(incoming.getRemaining(), waiting.getRemaining());
            if (qty <= 0) {
                break;
            }
            double waitingPrice = waiting.getPrice();
            double incomingPayPrice = r2(event.getD() - waitingPrice);
            mintPair(event, incomingOption, incoming, other, waiting, qty, incomingPayPrice, waitingPrice);
            minted = true;
        } while (minted && incoming.getRemaining() > 0);
    }

    private void mintPair(Event event, Option incomingOption, BookOrder incoming,
                          Option waitingOption, BookOrder waiting, int qty,
                          double incomingPayPrice, double waitingPrice) {
        User incomingUser = findUser(incoming.getUserName());
        User waitUser = findUser(waiting.getUserName());

        double payIncoming = r2(qty * incomingPayPrice);
        double payWaiting = r2(qty * waitingPrice);
        double incomingLimit = r2(qty * incoming.getPrice());

        incomingUser.consumeReserved(payIncoming);
        incoming.setReservedCash(r2(incoming.getReservedCash() - incomingLimit));
        incomingUser.releaseCash(r2(incomingLimit - payIncoming));

        waitUser.consumeReserved(payWaiting);
        waiting.setReservedCash(r2(waiting.getReservedCash() - r2(qty * waiting.getPrice())));

        event.getAccount().addInvestment(r2(payIncoming + payWaiting));

        UserPosition inPos = incomingUser.position(event.getId());
        UserPosition waitPos = waitUser.position(event.getId());
        inPos.addShares(incomingOption.getOptionName(), qty);
        waitPos.addShares(waitingOption.getOptionName(), qty);
        inPos.addPaid(payIncoming);
        waitPos.addPaid(payWaiting);
        incomingOption.setShareBought(incomingOption.getShareBought() + qty);
        waitingOption.setShareBought(waitingOption.getShareBought() + qty);

        double commIn = r2(calculateCommissionAmount(event, payIncoming, "on-purchase"));
        double commWait = r2(calculateCommissionAmount(event, payWaiting, "on-purchase"));
        if (commIn > 0) {
            incomingUser.requireFunds(commIn);
            incomingUser.debit(commIn, "Mint commission");
            payCommissionToMm(event, commIn);
            inPos.addCommission(commIn);
        }
        if (commWait > 0) {
            waitUser.requireFunds(commWait);
            waitUser.debit(commWait, "Mint commission");
            payCommissionToMm(event, commWait);
            waitPos.addCommission(commWait);
        }

        incoming.setRemaining(incoming.getRemaining() - qty);
        waiting.setRemaining(waiting.getRemaining() - qty);
        if (waiting.getRemaining() == 0) {
            event.bookOf(waitingOption.getOptionName()).remove(waiting);
        }
        event.bookOf(incomingOption.getOptionName()).setLastPrice(incomingPayPrice);
        event.bookOf(waitingOption.getOptionName()).setLastPrice(waitingPrice);
        event.addTrade(new Trade(incomingUser.getName(), incomingOption.getOptionName(), qty, payIncoming, commIn));
        event.addTrade(new Trade(waitUser.getName(), waitingOption.getOptionName(), qty, payWaiting, commWait));
    }

    private Option otherOption(Event event, Option option) {
        for (Option other : event.getOptions()) {
            if (!other.getOptionName().equals(option.getOptionName())) {
                return other;
            }
        }
        throw new IllegalArgumentException("Event must have two options.");
    }

    private void cancelAllOpenOrders(Event event) {
        for (Option option : event.getOptions()) {
            OptionBook book = event.bookOf(option.getOptionName());
            List<BookOrder> all = new ArrayList<>();
            all.addAll(book.getBids());
            all.addAll(book.getAsks());
            for (BookOrder order : all) {
                User user = findUser(order.getUserName());
                if (order.isBuy()) {
                    user.releaseCash(order.getReservedCash());
                    order.setReservedCash(0);
                } else {
                    user.position(event.getId()).removeReservedShares(option.getOptionName(), order.getRemaining());
                }
                book.remove(order);
            }
        }
    }

    private void payCommissionToMm(Event event, double commission) {
        if (commission <= 0) {
            return;
        }
        event.getAccount().recordCommissionOnly(commission);
        if (hasUsers()) {
            findMarketMaker(event).credit(commission, "Commission received");
        } else {
            event.getAccount().addInvestment(commission);
        }
    }

    private void requireActive(Event event) {
        if (event.getStatus() == EventStatus.NOT_STARTED) {
            throw new IllegalArgumentException("Event " + event.getId() + " has not been opened by the market maker yet.");
        }
        if (event.getStatus() == EventStatus.CLOSED) {
            throw new IllegalArgumentException("Event with ID " + event.getId() + " is already closed for trading.");
        }
    }

    private BuyResultDTO blockedMessage(User user, BuyResultDTO result) {
        if (user.isBlocked()) {
            return new BuyResultDTO(result.getBasePrice(), result.getCommission(),
                    "User '" + user.getName() + "' now has a negative balance and is blocked.");
        }
        return result;
    }

    private BuyResultDTO buySharesAnonymous(int eventId, String optionName, int amount) {
        Event currentEvent = findLogicEventById(eventId);
        if (!currentEvent.isLmsr()) {
            throw new IllegalArgumentException("Order Book trading requires named users.");
        }
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
        double oldCost = calculateMarketCost(currentEvent, selectedOption.getOptionName(), 0);
        double newCost = calculateMarketCost(currentEvent, selectedOption.getOptionName(), amount);
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

    private void closeEventAnonymous(int eventId, String winningOptionName) {
        Event currentEvent = findLogicEventById(eventId);
        if (!currentEvent.isLmsr()) {
            throw new IllegalArgumentException("Order Book trading requires named users.");
        }
        if (!currentEvent.isActive()) {
            throw new IllegalArgumentException("Event with ID " + eventId + " is already closed for trading.");
        }
        Option winningOption = findOptionByName(currentEvent, winningOptionName);
        if (winningOption == null) {
            throw new IllegalArgumentException(
                    "Error: The winning option '" + winningOptionName + "' does not exist in this event.");
        }
        int winningShares = winningOption.getShareBought();
        currentEvent.setStatus(EventStatus.CLOSED);
        currentEvent.setWinningOptionName(winningOption.getOptionName());
        double payout = winningShares * 1.0;
        double closeCommission = calculateCommissionAmount(currentEvent, payout, "on-close");
        if (closeCommission > 0) {
            currentEvent.getAccount().addCommission(closeCommission);
        }
        currentEvent.getAccount().deductFunds(payout);
    }

    private double calculateCommissionAmount(Event event, double baseAmount, String currentAction) {
        if (currentAction.equalsIgnoreCase(event.getCommissionType())) {
            return baseAmount * (event.getCommission() / 100.0);
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
        if (!event.isLmsr() || event.getB() <= 0) {
            OptionBook book = event.bookOf(targetOption.getOptionName());
            if (book.getLastPrice() != null) {
                return book.getLastPrice() / event.getD();
            }
            if (book.mid() != null) {
                return book.mid() / event.getD();
            }
            return 0.5;
        }
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
            OptionBook book = event.bookOf(opt.getOptionName());
            List<OrderDTO> bids = new ArrayList<>();
            List<OrderDTO> asks = new ArrayList<>();
            if (book != null) {
                for (BookOrder order : book.getBids()) {
                    bids.add(new OrderDTO(order.getUserName(), "BUY", order.getRemaining(), order.getPrice()));
                }
                for (BookOrder order : book.getAsks()) {
                    asks.add(new OrderDTO(order.getUserName(), "SELL", order.getRemaining(), order.getPrice()));
                }
            }
            optionDTOs.add(new OptionDTO(
                    opt.getOptionName(),
                    opt.getShareBought(),
                    calculateOptionProbability(event, opt),
                    book == null ? null : book.getLastPrice(),
                    book == null ? null : book.bestBidPrice(),
                    book == null ? null : book.bestAskPrice(),
                    book == null ? null : book.mid(),
                    book == null ? null : book.spread(),
                    bids,
                    asks
            ));
        }

        List<TradeDTO> tradeDTOs = new ArrayList<>();
        for (Trade trade : event.getTradeHistory()) {
            tradeDTOs.add(new TradeDTO(
                    trade.getUserName(),
                    trade.getOptionName(),
                    trade.getQuantity(),
                    trade.getPricePaid(),
                    trade.getCommission()
            ));
        }

        List<ParticipantDTO> participants = new ArrayList<>();
        for (User user : users) {
            if (!user.hasParticipated(event.getId()) && !user.isMarketMakerOf(event.getId())) {
                continue;
            }
            if (!user.hasParticipated(event.getId()) && event.getStatus() == EventStatus.NOT_STARTED) {
                continue;
            }
            if (!user.hasParticipated(event.getId())) {
                continue;
            }
            UserPosition pos = user.position(event.getId());
            StringBuilder holdings = new StringBuilder();
            for (Option opt : event.getOptions()) {
                if (holdings.length() > 0) {
                    holdings.append(" | ");
                }
                holdings.append(opt.getOptionName()).append(": ").append(pos.getShares(opt.getOptionName()));
            }
            participants.add(new ParticipantDTO(user.getName(), holdings.toString(), pos.getTotalCommission()));
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
                event.getWinningOptionName(),
                statusText(event),
                event.isLmsr() ? "LMSR" : "Order Book",
                event.getMarketMakerName(),
                participants,
                event.isAllowMint(),
                event.getD(),
                event.getInitialInvestment()
        );
    }

    private String statusText(Event event) {
        if (event.getStatus() == EventStatus.NOT_STARTED) {
            return "Not started";
        }
        if (event.getStatus() == EventStatus.CLOSED) {
            return "Closed";
        }
        return "Active";
    }

    private UserDTO toUserDTO(User user) {
        List<UserEventDTO> parts = new ArrayList<>();
        for (Event event : events) {
            boolean mm = user.isMarketMakerOf(event.getId());
            boolean participated = user.hasParticipated(event.getId());
            if (!mm && !participated) {
                continue;
            }
            String involvement = mm ? "Market maker" : "Participant";
            if (mm && participated) {
                involvement = "Market maker + participant";
            }
            String details;
            if (participated) {
                UserPosition pos = user.position(event.getId());
                StringBuilder sb = new StringBuilder();
                for (Option opt : event.getOptions()) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(opt.getOptionName()).append(" shares=").append(pos.getShares(opt.getOptionName()));
                }
                sb.append(String.format("%nPaid: %.2f, Commission: %.2f", pos.getTotalPaid(), pos.getTotalCommission()));
                if (event.getStatus() == EventStatus.CLOSED) {
                    sb.append(String.format("%nWinner: %s", event.getWinningOptionName()));
                    if (pos.getClosedPnl() != null) {
                        sb.append(String.format("%nP/L: %.2f", pos.getClosedPnl()));
                    }
                    if (event.isLmsr()) {
                        sb.append("\nTrade history (newest first):");
                        for (Trade trade : event.getTradeHistory()) {
                            if (trade.getUserName() == null || trade.getUserName().equalsIgnoreCase(user.getName())) {
                                sb.append(String.format("%n  %s x%d paid %.2f commission %.2f",
                                        trade.getOptionName(), trade.getQuantity(),
                                        trade.getPricePaid(), trade.getCommission()));
                            }
                        }
                    }
                }
                details = sb.toString();
            } else {
                details = "Assigned as market maker. Event is " + statusText(event) + ".";
            }
            parts.add(new UserEventDTO(
                    event.getId(),
                    event.getName(),
                    event.isLmsr() ? "LMSR" : "Order Book",
                    involvement,
                    details
            ));
        }
        return new UserDTO(
                user.getName(),
                user.getDisplayedCash(),
                user.isBlocked(),
                new ArrayList<>(user.getMmEventIds()),
                parts,
                new ArrayList<>(user.getLedger())
        );
    }

    private void validateLoadedDescriptor(GuessMarketDescriptor descriptor, boolean ex2) {
        if (descriptor.getEvents() == null || descriptor.getEvents().isEmpty()) {
            throw new IllegalArgumentException("Error: The XML file must contain at least one event.");
        }
        if (ex2 && (descriptor.getUsers() == null || descriptor.getUsers().isEmpty())) {
            throw new IllegalArgumentException("Error: The XML file must contain at least one user.");
        }

        Set<Integer> eventIds = new HashSet<>();
        for (GMEvent event : descriptor.getEvents()) {
            if (!eventIds.add(event.getId())) {
                throw new IllegalArgumentException("Error: Event with duplicate ID found: " + event.getId());
            }
            int c = event.getCommission().getValue();
            if (c < 0 || c > 90) {
                throw new IllegalArgumentException("Error: Commission must be between 0 and 90. Found: " + c);
            }
            if (event.getOptions() == null || event.getOptions().size() != 2) {
                throw new IllegalArgumentException("Error: Each event must have exactly 2 options. Event ID: " + event.getId());
            }
            if (event.getMethod() == null) {
                throw new IllegalArgumentException("Error: Each event must have a method. Event ID: " + event.getId());
            }
            boolean lmsr = event.getMethod().getLmsr() != null;
            boolean orderBook = event.getMethod().getOrderBook() != null;
            if (lmsr == orderBook) {
                throw new IllegalArgumentException("Error: Each event must have exactly one method. Event ID: " + event.getId());
            }
            if (lmsr && event.getMethod().getLmsr().getB() <= 0) {
                throw new IllegalArgumentException("Error: B must be greater than 0 for LMSR method. Found: " + event.getMethod().getLmsr().getB());
            }
            if (orderBook) {
                if (event.getMethod().getOrderBook().getD() <= 0) {
                    throw new IllegalArgumentException("Error: D must be greater than 0 for Order Book method. Found: " + event.getMethod().getOrderBook().getD());
                }
                if (event.getMethod().getOrderBook().getInitial() < 0) {
                    throw new IllegalArgumentException("Order Book initial cannot be negative. Event " + event.getId());
                }
            }
        }

        if (!ex2) {
            return;
        }

        Set<String> userNames = new HashSet<>();
        Map<Integer, Integer> mmCountPerEvent = new HashMap<>();
        for (GMUser user : descriptor.getUsers()) {
            String name = user.getName() == null ? "" : user.getName().trim();
            if (name.isEmpty() || !userNames.add(name.toLowerCase())) {
                throw new IllegalArgumentException("User names must be unique and non-empty. Found: " + name);
            }
            if (user.getInitialCash() <= 0) {
                throw new IllegalArgumentException("User '" + name + "' initial cash must be > 0. Found: " + user.getInitialCash());
            }
            if (user.getMarketMakerEvents() == null) {
                continue;
            }
            for (GMMarketMakerEvent mm : user.getMarketMakerEvents()) {
                int eid = mm.getId();
                if (!eventIds.contains(eid)) {
                    throw new IllegalArgumentException(
                            "User '" + name + "' is MM of event " + eid + " which does not exist.");
                }
                mmCountPerEvent.put(eid, mmCountPerEvent.getOrDefault(eid, 0) + 1);
            }
        }
        for (Integer eid : eventIds) {
            int count = mmCountPerEvent.getOrDefault(eid, 0);
            if (count != 1) {
                throw new IllegalArgumentException(
                        "Event " + eid + " must have exactly 1 market maker. Found: " + count);
            }
        }
    }
}
