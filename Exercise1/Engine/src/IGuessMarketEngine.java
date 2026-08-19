import dto.BuyResultDTO;
import dto.EventDTO;
import java.util.List;

public interface IGuessMarketEngine {

    void loadXmlFile(String filePath);

    boolean hasLoadedData();

    List<EventDTO> getAllEvents();

    List<EventDTO> getActiveEvents();

    EventDTO getEventById(int id);

    BuyResultDTO buyShares(int eventId, String optionName, int amount);

    void closeEvent(int eventId, String winningOptionName);
}
