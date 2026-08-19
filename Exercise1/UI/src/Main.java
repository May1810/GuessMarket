import java.util.Scanner;
import java.util.List;
import dto.EventDTO;
import dto.OptionDTO;
import dto.TradeDTO;
import dto.BuyResultDTO;

public class Main {

    private static Scanner scanner = new Scanner(System.in);
    private static IGuessMarketEngine engine = new GuessMarketEngine();


    public static void main(String[] args){
        System.out.println("Welcome to the Guess Market System!");
        boolean isRunning = true;

        while(isRunning){
            printMenu();

            int choice = readInt("Please enter your choice (1-6): ");
            switch (choice){
                case 1:
                    System.out.print("Please enter the full path to the XML file: ");
                    String path = scanner.nextLine().trim();
                    try {
                        engine.loadXmlFile(path); // קוראים למנוע שיעשה את העבודה הקשה!
                        System.out.println("File loaded successfully!");
                    } catch (Exception e) {
                        System.out.println("Error loading file: " + e.getMessage());
                    }
                    break;
                case 2:
                    if (requireLoaded()) {
                        showAllEvents();
                    }
                    break;
                case 3:
                    if (requireLoaded()) {
                        showEventStateCommand();
                    }
                    break;
                case 4:
                    if (requireLoaded()) {
                        participateCommand();
                    }
                    break;
                case 5:
                    if (requireLoaded()) {
                        closeEventCommand();
                    }
                    break;
                case 6:
                    System.out.println("Goodbye! Exiting system...");
                    isRunning = false; // זה עוצר את הלולאה!
                    break;
                default:
                    System.out.println("Error: Invalid choice. Please select a number between 1 and 6.");
            }
            System.out.println();

        }
    }

    private static int readInt(String prompt) {
        while(true){
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try{
                return Integer.parseInt(input);
            }catch (NumberFormatException e){
                System.out.println("Error: That is not a valid number. Please try again.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("=================================");
        System.out.println("1. Load XML file");
        System.out.println("2. Show events");
        System.out.println("3. Show event trading state");
        System.out.println("4. Participate (buy shares)");
        System.out.println("5. Close event");
        System.out.println("6. Exit");
        System.out.println("=================================");
    }

    private static boolean requireLoaded(){
        if(!engine.hasLoadedData()){
            System.out.println("Error: No data loaded. Please load an XML file first (Command 1).");
            return false;
        }
        return true;
    }

    private static void showAllEvents() {
        List<EventDTO> events = engine.getAllEvents();

        System.out.println("\n--- All Events in System ---");

        for (EventDTO event : events) {
            System.out.println("ID: " + event.getId());
            System.out.println("Name: " + event.getName());
            System.out.println("Description: " + event.getDescription());
            System.out.println("Status: " + (event.isActive() ? "Active" : "Closed"));
            System.out.println("Commission: " + event.getCommission() + "% (" + event.getCommissionType() + ")");

            // הדפסת שמות האפשרויות בשורה אחת (למשל: Options: Yes, No)
            System.out.print("Options: ");
            for (int i = 0; i < event.getOptions().size(); i++) {
                System.out.print(event.getOptions().get(i).getOptionName());
                if (i < event.getOptions().size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println(); // יורדים שורה בסוף האפשרויות
            System.out.println("---------------------------------");
        }
    }

    // --- פקודה 3: הצגת מצב אירוע ---

    private static void showEventStateCommand() {
        List<EventDTO> allEvents = engine.getAllEvents();

        // נותנים למשתמש לבחור אירוע מתוך הרשימה
        EventDTO selected = pickEventFromList(allEvents, "Select an event to view its state:");

        if (selected != null) {
            // הולכים למנוע להביא את הגרסה הכי עדכנית של האירוע בעזרת ה-ID שלו
            EventDTO freshEventData = engine.getEventById(selected.getId());
            printEventState(freshEventData);
        }
    }

    // פונקציית עזר: מציגה רשימה ממוספרת של אירועים (1, 2, 3...) ומבקשת בחירה
    private static EventDTO pickEventFromList(List<EventDTO> events, String promptMessage) {
        if (events.isEmpty()) {
            System.out.println("No events available.");
            return null;
        }

        System.out.println("\n--- " + promptMessage + " ---");
        for (int i = 0; i < events.size(); i++) {
            System.out.println((i + 1) + ". " + events.get(i).getName());
        }

        while (true) {
            int choice = readInt("Enter your choice (1-" + events.size() + "): ");
            if (choice >= 1 && choice <= events.size()) {
                // המשתמש הקיש 1, אבל במערך זה אינדקס 0!
                return events.get(choice - 1);
            } else {
                System.out.println("Invalid choice. Please select a number from the list.");
            }
        }
    }

    private static void printEventState(EventDTO event) {
        System.out.println("\n========== Event State ==========");
        System.out.println("Name: " + event.getName());
        System.out.println("Status: " + (event.isActive() ? "Active" : "Closed"));

        System.out.println("\n--- Options ---");
        for (OptionDTO opt : event.getOptions()) {
            // שימוש ב-printf כדי להדפיס את ההסתברות עם 2 נקודות עשרוניות בלבד
            System.out.printf("- %s: %d shares (Probability: %.2f)\n",
                    opt.getOptionName(), opt.getShareBought(), opt.getCurrentValue());
        }

        System.out.println("\n--- Financials ---");
        System.out.printf("Account Balance: %.2f\n", event.getAccountBalance());
        System.out.printf("Total Commission Collected: %.2f\n", event.getTotalCommissionCollected());

        System.out.println("\n--- Trade History ---");
        if (event.getTradeHistory().isEmpty()) {
            System.out.println("No trades have been made yet.");
        } else {
            for (TradeDTO trade : event.getTradeHistory()) {
                System.out.printf("Bought %d shares of '%s' for %.2f\n",
                        trade.getQuantity(), trade.getOptionName(), trade.getPricePaid());
            }
        }

        if (!event.isActive() && event.getWinningOptionName() != null) {
            System.out.println("\n--- Closed Event Summary ---");
            for (OptionDTO opt : event.getOptions()) {
                System.out.println("Total shares of '" + opt.getOptionName() + "': " + opt.getShareBought());
            }
            System.out.println("Winning option: " + event.getWinningOptionName());
        }
        System.out.println("=================================");
    }

    private static OptionDTO pickOptionFromList(List<OptionDTO> options, String promptMessage) {
        if (options == null || options.isEmpty()) {
            System.out.println("No options available.");
            return null;
        }

        System.out.println("\n--- " + promptMessage + " ---");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i).getOptionName());
        }

        while (true) {
            int choice = readInt("Enter your choice (1-" + options.size() + "): ");
            if (choice >= 1 && choice <= options.size()) {
                return options.get(choice - 1);
            }
            System.out.println("Invalid choice. Please select a number from the list.");
        }
    }

    // --- פקודה 4: קניית מניות ---

    private static void participateCommand() {
        // 1. מבקשים מהמנוע *רק* אירועים פעילים
        List<EventDTO> activeEvents = engine.getActiveEvents();

        if (activeEvents.isEmpty()) {
            System.out.println("No active events available for trading at the moment.");
            return;
        }

        // 2. בחירת אירוע בעזרת הפונקציה החכמה שכבר כתבנו
        EventDTO selectedEvent = pickEventFromList(activeEvents, "Select an active event to participate in");
        if (selectedEvent == null) return;

        EventDTO freshEvent = engine.getEventById(selectedEvent.getId());
        printEventState(freshEvent);

        OptionDTO selectedOption = pickOptionFromList(freshEvent.getOptions(), "Select Option");
        if (selectedOption == null) return;
        String selectedOptionName = selectedOption.getOptionName();

        int amount;
        while (true) {
            amount = readInt("Enter amount of shares to buy: ");
            if (amount > 0) {
                break;
            }
            System.out.println("Amount must be greater than 0.");
        }

        try {
            BuyResultDTO receipt = engine.buyShares(freshEvent.getId(), selectedOptionName, amount);

            System.out.println("\n=================================");
            System.out.println("       PURCHASE SUCCESSFUL!      ");
            System.out.println("=================================");
            System.out.printf("Base Price: %.2f\n", receipt.getBasePrice());
            System.out.printf("Commission: %.2f\n", receipt.getCommission());
            System.out.printf("Total Paid: %.2f\n", receipt.getTotalPaid());
            System.out.println("=================================");

            // הדפסת המצב המעודכן לאחר הקנייה
            System.out.println("\n--- Updated Event State ---");
            printEventState(engine.getEventById(freshEvent.getId()));

        } catch (Exception e) {
            System.out.println("Error during purchase: " + e.getMessage());
        }
    }

    // --- פקודה 5: סגירת אירוע ---

    private static void closeEventCommand() {
        // מביאים רק אירועים פעילים! אי אפשר לסגור אירוע שכבר נסגר
        List<EventDTO> activeEvents = engine.getActiveEvents();

        if (activeEvents.isEmpty()) {
            System.out.println("No active events available to close.");
            return;
        }

        // בחירת אירוע
        EventDTO selectedEvent = pickEventFromList(activeEvents, "Select an active event to close");
        if (selectedEvent == null) return;

        EventDTO freshEvent = engine.getEventById(selectedEvent.getId());
        printEventState(freshEvent);

        OptionDTO winningOption = pickOptionFromList(freshEvent.getOptions(), "Select Winning Option");
        if (winningOption == null) return;
        String winningOptionName = winningOption.getOptionName();

        try {
            engine.closeEvent(freshEvent.getId(), winningOptionName);

            System.out.println("\n=================================");
            System.out.println("   EVENT CLOSED SUCCESSFULLY!    ");
            System.out.println("=================================");

            // הדפסת המצב המעודכן לאחר הסגירה
            System.out.println("\n--- Final Event Summary ---");
            printEventState(engine.getEventById(freshEvent.getId()));

        } catch (Exception e) {
            System.out.println("Error closing event: " + e.getMessage());
        }
    }
}
