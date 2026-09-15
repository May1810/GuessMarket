import dto.BuyResultDTO;
import dto.EventDTO;
import dto.UserDTO;
import java.util.List;

public interface IGuessMarketEngine {

    void loadXmlFile(String filePath);

    boolean hasLoadedData();

    List<EventDTO> getAllEvents();

    List<EventDTO> getActiveEvents();

    EventDTO getEventById(int id);

    BuyResultDTO buyShares(int eventId, String optionName, int amount);

    void closeEvent(int eventId, String winningOptionName);

    List<UserDTO> getAllUsers();

    UserDTO getUserByName(String name);

    void openEvent(String userName, int eventId);

    void closeEvent(String userName, int eventId, String winningOptionName);

    BuyResultDTO buyShares(String userName, int eventId, String optionName, int amount);

    BuyResultDTO placeOrder(String userName, int eventId, String optionName, String side, int quantity, double price);
}
