import com.google.gson.Gson;
import dto.BuyResultDTO;
import dto.EventDTO;
import dto.OptionDTO;
import dto.OrderDTO;
import dto.ParticipantDTO;
import dto.UserDTO;
import dto.UserEventDTO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainController {

    private final IGuessMarketEngine engine = new GuessMarketEngine();
    private Timeline refreshTimeline;
    private boolean eventsRequestRunning;
    private boolean usersRequestRunning;

    @FXML private Button loadButton;
    @FXML private ProgressBar loadProgress;
    @FXML private Label loadedPathLabel;
    @FXML private TableView<EventDTO> eventsTable;
    @FXML private TableColumn<EventDTO, Integer> idColumn;
    @FXML private TableColumn<EventDTO, String> nameColumn;
    @FXML private TableColumn<EventDTO, String> methodColumn;
    @FXML private TableColumn<EventDTO, String> statusColumn;
    @FXML private TableColumn<EventDTO, Integer> commissionColumn;
    @FXML private TableColumn<EventDTO, String> commissionTypeColumn;
    @FXML private TableColumn<EventDTO, String> accountColumn;
    @FXML private ToggleButton methodAll;
    @FXML private ToggleButton methodLmsr;
    @FXML private ToggleButton methodOb;
    @FXML private ToggleButton statusAll;
    @FXML private ToggleButton statusNotStarted;
    @FXML private ToggleButton statusActive;
    @FXML private ToggleButton statusClosed;
    @FXML private ToggleButton commissionAll;
    @FXML private ToggleButton commissionPurchase;
    @FXML private ToggleButton commissionClose;
    @FXML private Label eventSummaryLabel;
    @FXML private TextArea option1Area;
    @FXML private TextArea option2Area;
    @FXML private Label participationLabel;
    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> userNameColumn;
    @FXML private TableColumn<UserDTO, String> userCashColumn;
    @FXML private TableColumn<UserDTO, String> userMmColumn;
    @FXML private TableColumn<UserDTO, String> userBlockedColumn;
    @FXML private Label myCashLabel;
    @FXML private TextField topupField;
    @FXML private TextArea ledgerArea;
    @FXML private Label userDetailsLabel;
    @FXML private TableView<UserEventDTO> userEventsTable;
    @FXML private TableColumn<UserEventDTO, Integer> userEventIdColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventNameColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventTypeColumn;
    @FXML private TableColumn<UserEventDTO, String> userEventRoleColumn;
    @FXML private Label userEventDetailsLabel;
    @FXML private ComboBox<EventDTO> tradeEventCombo;
    @FXML private ComboBox<String> optionCombo;
    @FXML private ComboBox<String> sideCombo;
    @FXML private ComboBox<String> winnerCombo;
    @FXML private TextField amountField;
    @FXML private TextField priceField;
    @FXML private Label tradeResultLabel;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("methodType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        commissionColumn.setCellValueFactory(new PropertyValueFactory<>("commission"));
        commissionTypeColumn.setCellValueFactory(new PropertyValueFactory<>("commissionType"));
        accountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                String.format("%.2f", cell.getValue().getAccountBalance())));

        eventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                showEventDetails(selected);
            }
        });

        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        userCashColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                String.format("%.2f", cell.getValue().getCash())));
        userMmColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().isMarketMaker() ? "Yes" : "No"));
        userBlockedColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().isBlocked() ? "Yes" : "No"));

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                showUserDetails(selected);
            }
        });

        userEventIdColumn.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        userEventNameColumn.setCellValueFactory(new PropertyValueFactory<>("eventName"));
        userEventTypeColumn.setCellValueFactory(new PropertyValueFactory<>("methodType"));
        userEventRoleColumn.setCellValueFactory(new PropertyValueFactory<>("involvement"));
        userEventsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                userEventDetailsLabel.setText(selected.getDetails());
                selectTradeEventById(selected.getEventId());
            }
        });

        tradeEventCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(EventDTO event) {
                return event == null ? "" : event.getId() + " - " + event.getName() + " (" + event.getStatus() + ")";
            }

            @Override
            public EventDTO fromString(String string) {
                return null;
            }
        });
        tradeEventCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            fillOptionCombos(selected);
        });

        sideCombo.getItems().setAll("Buy", "Sell");
        sideCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
        applyEventFilter();
    }

    public void startAutoRefresh() {
        if (refreshTimeline != null) {
            return;
        }
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            loadEventsFromServer(false);
            loadUsersFromServer(false);
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    public void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    public void loadEventsFromServer() {
        loadEventsFromServer(true);
    }

    private void loadEventsFromServer(boolean showErrors) {
        if (eventsRequestRunning) {
            return;
        }
        eventsRequestRunning = true;
        Integer selectedTableId = eventsTable.getSelectionModel().getSelectedItem() == null
                ? null : eventsTable.getSelectionModel().getSelectedItem().getId();
        Integer selectedComboId = tradeEventCombo.getValue() == null
                ? null : tradeEventCombo.getValue().getId();
        String selectedOption = optionCombo.getValue();
        String selectedWinner = winnerCombo.getValue();

        Task<List<EventDTO>> task = new Task<>() {
            @Override
            protected List<EventDTO> call() throws Exception {
                // בונים את בקשת הרשת לכתובת של השרת
                // * הערה: תוודאי ש-ServerConfig.BASE באמת קיים אצלך, או שתחליפי בכתובת המלאה
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.BASE + "/events"))
                        .GET()
                        .build();

                // שולחים את הבקשה
                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new IllegalStateException(response.body());
                }

                // מתרגמים את ה-JSON שחזר למערך של EventDTO
                EventDTO[] events = new Gson().fromJson(response.body(), EventDTO[].class);
                return events == null ? List.of() : List.of(events);
            }
        };

        // ברגע שהמשימה הצליחה - מעדכנים את הטבלה (זה רץ בטוח על ה-JavaFX Thread)
        // * הערה: תוודאי שלטבלה שלך קוראים eventsTable, אחרת תשני לשם שלה
        task.setOnSucceeded(e -> {
            eventsRequestRunning = false;
            List<EventDTO> events = task.getValue();
            eventsTable.getItems().setAll(events);
            tradeEventCombo.getItems().setAll(events);
            if (selectedTableId != null) {
                for (EventDTO row : events) {
                    if (row.getId() == selectedTableId) {
                        eventsTable.getSelectionModel().select(row);
                        break;
                    }
                }
            }
            if (selectedComboId != null) {
                for (EventDTO row : events) {
                    if (row.getId() == selectedComboId) {
                        tradeEventCombo.getSelectionModel().select(row);
                        break;
                    }
                }
                if (selectedOption != null && optionCombo.getItems().contains(selectedOption)) {
                    optionCombo.getSelectionModel().select(selectedOption);
                }
                if (selectedWinner != null && winnerCombo.getItems().contains(selectedWinner)) {
                    winnerCombo.getSelectionModel().select(selectedWinner);
                }
            }
        });

        task.setOnFailed(e -> {
            eventsRequestRunning = false;
            if (showErrors) {
                showError("Events Error", task.getException().getMessage());
            }
        });

        // מריצים את הכל בשרשור (Thread) נפרד כדי לא להקפיא את המסך
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onLoadClick(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        File file = chooser.showOpenDialog(loadButton.getScene().getWindow());
        if (file == null) {
            return;
        }

        loadButton.setDisable(true);
        loadProgress.setVisible(true);
        loadProgress.setProgress(-1);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                String boundary = "----GuessMarket" + System.currentTimeMillis();

                // **כאן לעדכן:** תשני למשתנה שבו את שומרת את שם המשתמש שהתחבר!
                String user = Session.userName;

                ByteArrayOutputStream body = new ByteArrayOutputStream();
                String header = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"username\"\r\n\r\n"
                        + user + "\r\n"
                        + "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n"
                        + "Content-Type: application/xml\r\n\r\n";

                body.write(header.getBytes(StandardCharsets.UTF_8));
                body.write(fileBytes);
                body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.BASE + "/upload"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    Map<String, Object> responseMap = new Gson().fromJson(response.body(), Map.class);
                    String errorMsg = responseMap.containsKey("message") ? (String) responseMap.get("message") : "Upload failed";
                    throw new IllegalStateException(errorMsg);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            loadedPathLabel.setText(file.getAbsolutePath());
            tradeResultLabel.setText("");

            // כאן הקסם: במקום refreshAll ישן, אנחנו מושכים את הנתונים העדכניים מהשרת!
            loadEventsFromServer();

            finishLoadUi(); // מחזיר את הכפתור והפרוגרס למצב רגיל
        });

        task.setOnFailed(e -> {
            finishLoadUi();
            showError("Could not load file", task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onOpenClick(ActionEvent event) {
        EventDTO selected = tradeEventCombo.getValue();
        if (selected == null || Session.userName == null) {
            showError("Open event", "Select an event.");
            return;
        }
        String body = "username=" + enc(Session.userName) + "&eventId=" + selected.getId();
        postForm("/open", body, "Event opened.");
    }

    @FXML
    private void onCloseClick(ActionEvent event) {
        EventDTO selected = tradeEventCombo.getValue();
        String winner = winnerCombo.getValue();
        if (selected == null || Session.userName == null) {
            showError("Close event", "Select an event.");
            return;
        }
        if (winner == null || winner.isBlank()) {
            showError("Close event", "Select the winning option.");
            return;
        }
        String body = "username=" + enc(Session.userName)
                + "&eventId=" + selected.getId()
                + "&winningOption=" + enc(winner);
        postForm("/close", body, "Event closed.");
    }

    @FXML
    private void onBuyClick(ActionEvent event) {
        EventDTO selected = tradeEventCombo.getValue();
        String optionName = optionCombo.getValue();
        if (selected == null || Session.userName == null) {
            showError("Buy", "Select an event.");
            return;
        }
        if (optionName == null || optionName.isBlank()) {
            showError("Buy", "Select an option.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(amountField.getText().trim());
        } catch (Exception e) {
            showError("Buy", "Amount must be a whole number.");
            return;
        }
        String body = "username=" + enc(Session.userName)
                + "&eventId=" + selected.getId()
                + "&optionName=" + enc(optionName)
                + "&amount=" + amount;
        postForm("/buy", body, "Buy completed.");
    }

    private void postForm(String path, String formBody, String successText) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.BASE + path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody))
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException(messageFrom(response.body()));
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            tradeResultLabel.setText(successText);
            loadEventsFromServer();
        });
        task.setOnFailed(e -> showError("Action failed", task.getException().getMessage()));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String messageFrom(String json) {
        String key = "\"message\":\"";
        int start = json.indexOf(key);
        if (start < 0) {
            return json;
        }
        start += key.length();
        int end = json.indexOf('"', start);
        return end < 0 ? json : json.substring(start, end);
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @FXML
    private void onPlaceOrderClick(ActionEvent event) {
        EventDTO selected = tradeEventCombo.getValue();
        String optionName = optionCombo.getValue();
        String side = sideCombo.getValue();
        if (selected == null || Session.userName == null) {
            showError("Order", "Select an event.");
            return;
        }
        if (optionName == null || optionName.isBlank()) {
            showError("Order", "Select an option.");
            return;
        }
        if (side == null || side.isBlank()) {
            showError("Order", "Select Buy or Sell.");
            return;
        }
        int qty;
        double price;
        try {
            qty = Integer.parseInt(amountField.getText().trim());
            price = Double.parseDouble(priceField.getText().trim());
        } catch (Exception e) {
            showError("Order", "Quantity must be a whole number and price must be numeric.");
            return;
        }
        String body = "username=" + enc(Session.userName)
                + "&eventId=" + selected.getId()
                + "&optionName=" + enc(optionName)
                + "&side=" + enc(side)
                + "&quantity=" + qty
                + "&price=" + price;
        postForm("/order", body, "Order processed.");
    }

    private void finishLoadUi() {
        loadButton.setDisable(false);
        loadProgress.setVisible(false);
        loadProgress.setProgress(0);
    }

    private UserDTO selectedUser() {
        return usersTable.getSelectionModel().getSelectedItem();
    }

    private void refreshAll(String keepUserName, Integer keepEventId) {
        applyEventFilter();
        usersTable.getItems().setAll(engine.getAllUsers());
        List<EventDTO> all = engine.getAllEvents();
        EventDTO previousTrade = tradeEventCombo.getValue();
        tradeEventCombo.getItems().setAll(all);
        if (keepEventId != null) {
            selectTradeEventById(keepEventId);
            selectEventRow(keepEventId);
            showEventDetails(engine.getEventById(keepEventId));
        } else if (previousTrade != null) {
            selectTradeEventById(previousTrade.getId());
        }
        if (keepUserName != null) {
            for (UserDTO user : usersTable.getItems()) {
                if (user.getName().equalsIgnoreCase(keepUserName)) {
                    usersTable.getSelectionModel().select(user);
                    showUserDetails(engine.getUserByName(keepUserName));
                    break;
                }
            }
        }
    }

    private void applyEventFilter() {
        if (!engine.hasLoadedData()) {
            eventsTable.getItems().clear();
            return;
        }
        Integer selectedId = eventsTable.getSelectionModel().getSelectedItem() == null
                ? null : eventsTable.getSelectionModel().getSelectedItem().getId();
        List<EventDTO> filtered = new ArrayList<>();
        for (EventDTO event : engine.getAllEvents()) {
            if (methodLmsr.isSelected() && !"LMSR".equals(event.getMethodType())) {
                continue;
            }
            if (methodOb.isSelected() && !"Order Book".equals(event.getMethodType())) {
                continue;
            }
            if (statusNotStarted.isSelected() && !"Not started".equals(event.getStatus())) {
                continue;
            }
            if (statusActive.isSelected() && !"Active".equals(event.getStatus())) {
                continue;
            }
            if (statusClosed.isSelected() && !"Closed".equals(event.getStatus())) {
                continue;
            }
            if (commissionPurchase.isSelected() && !"on-purchase".equalsIgnoreCase(event.getCommissionType())) {
                continue;
            }
            if (commissionClose.isSelected() && !"on-close".equalsIgnoreCase(event.getCommissionType())) {
                continue;
            }
            filtered.add(event);
        }
        eventsTable.getItems().setAll(filtered);
        if (selectedId != null) {
            selectEventRow(selectedId);
        }
    }

    private void selectEventRow(int eventId) {
        for (EventDTO row : eventsTable.getItems()) {
            if (row.getId() == eventId) {
                eventsTable.getSelectionModel().select(row);
                break;
            }
        }
    }

    private void selectTradeEventById(int eventId) {
        for (EventDTO event : tradeEventCombo.getItems()) {
            if (event.getId() == eventId) {
                tradeEventCombo.getSelectionModel().select(event);
                fillOptionCombos(engine.getEventById(eventId));
                return;
            }
        }
    }

    private void fillOptionCombos(EventDTO event) {
        optionCombo.getItems().clear();
        winnerCombo.getItems().clear();
        if (event == null || event.getOptions() == null) {
            return;
        }
        for (OptionDTO option : event.getOptions()) {
            optionCombo.getItems().add(option.getOptionName());
            winnerCombo.getItems().add(option.getOptionName());
        }
        if (!optionCombo.getItems().isEmpty()) {
            optionCombo.getSelectionModel().selectFirst();
            winnerCombo.getSelectionModel().selectFirst();
        }
    }

    private void showEventDetails(EventDTO event) {
        eventSummaryLabel.setText(String.format(
                "%s%nType: %s | Status: %s%nMM: %s%nCommission: %d%% (%s)%nAccount: %.2f | Total commission: %.2f%n%s%s",
                event.getName(),
                event.getMethodType(),
                event.getStatus(),
                event.getMarketMakerName() == null ? "-" : event.getMarketMakerName(),
                event.getCommission(),
                event.getCommissionType(),
                event.getAccountBalance(),
                event.getTotalCommissionCollected(),
                event.getDescription() == null ? "" : event.getDescription() + "\n",
                event.getWinningOptionName() == null ? "" : "Winner: " + event.getWinningOptionName()
        ));
        if (event.getOptions() != null && !event.getOptions().isEmpty()) {
            option1Area.setText(formatOption(event, event.getOptions().get(0)));
        }
        if (event.getOptions() != null && event.getOptions().size() >= 2) {
            option2Area.setText(formatOption(event, event.getOptions().get(1)));
        }
        StringBuilder parts = new StringBuilder();
        if (event.getParticipants() == null || event.getParticipants().isEmpty()) {
            parts.append("No participants yet.");
        } else {
            for (ParticipantDTO p : event.getParticipants()) {
                parts.append(p.getName()).append(" - ").append(p.getHoldingsSummary())
                        .append(String.format(" (commission %.2f)%n", p.getTotalCommission()));
            }
        }
        if (event.getTradeHistory() != null && !event.getTradeHistory().isEmpty()) {
            parts.append("\nTrades (newest first):\n");
            event.getTradeHistory().forEach(t -> parts.append(String.format(
                    "%s bought %d %s for %.2f (commission %.2f)%n",
                    t.getUserName() == null ? "?" : t.getUserName(),
                    t.getQuantity(), t.getOptionName(), t.getPricePaid(), t.getCommission())));
        }
        participationLabel.setText(parts.toString());
    }

    private String formatOption(EventDTO event, OptionDTO option) {
        StringBuilder sb = new StringBuilder();
        sb.append(option.getOptionName()).append("\n");
        sb.append("Shares in market: ").append(option.getShareBought()).append("\n");
        if ("LMSR".equals(event.getMethodType())) {
            sb.append(String.format("Probability: %.2f%n", option.getCurrentValue()));
        } else {
            sb.append(String.format("LAST: %s%nBID: %s%nASK: %s%nMID: %s%nSPREAD: %s%n",
                    fmt(option.getLastPrice()), fmt(option.getBidPrice()), fmt(option.getAskPrice()),
                    fmt(option.getMidPrice()), fmt(option.getSpread())));
            sb.append("Bids:\n");
            if (option.getBids() == null || option.getBids().isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (OrderDTO order : option.getBids()) {
                    sb.append(String.format("  %s BUY %d @ %.2f%n", order.getUserName(), order.getQuantity(), order.getPrice()));
                }
            }
            sb.append("Asks:\n");
            if (option.getAsks() == null || option.getAsks().isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (OrderDTO order : option.getAsks()) {
                    sb.append(String.format("  %s SELL %d @ %.2f%n", order.getUserName(), order.getQuantity(), order.getPrice()));
                }
            }
        }
        return sb.toString();
    }

    private String fmt(Double value) {
        return value == null ? "-" : String.format("%.2f", value);
    }

    private void showUserDetails(UserDTO user) {
        String mm = user.getMmEventIds() == null || user.getMmEventIds().isEmpty()
                ? "none" : user.getMmEventIds().toString();
        userDetailsLabel.setText(String.format(
                "Name: %s%nCash: %.2f%nBlocked: %s%nMarket maker of events: %s",
                user.getName(), user.getCash(), user.isBlocked() ? "Yes" : "No", mm));
        userEventsTable.getItems().setAll(user.getParticipations());
        if (user.getParticipations() != null && !user.getParticipations().isEmpty()) {
            userEventsTable.getSelectionModel().selectFirst();
            userEventDetailsLabel.setText(user.getParticipations().get(0).getDetails());
        } else {
            userEventDetailsLabel.setText("This user has not participated in an event yet.");
        }
    }

    public void loadUsersFromServer() {
        loadUsersFromServer(true);
    }

    private void loadUsersFromServer(boolean showErrors) {
        if (usersRequestRunning) {
            return;
        }
        usersRequestRunning = true;
        String selectedName = usersTable.getSelectionModel().getSelectedItem() == null
                ? null : usersTable.getSelectionModel().getSelectedItem().getName();
        Task<List<UserDTO>> task = new Task<>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.BASE + "/users"))
                        .GET()
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException(response.body());
                }
                UserDTO[] users = new Gson().fromJson(response.body(), UserDTO[].class);
                return users == null ? List.of() : List.of(users);
            }
        };
        task.setOnSucceeded(e -> {
            usersRequestRunning = false;
            List<UserDTO> users = task.getValue();
            usersTable.getItems().setAll(users);
            UserDTO me = null;
            for (UserDTO user : users) {
                if (Session.userName != null && user.getName().equalsIgnoreCase(Session.userName)) {
                    me = user;
                }
                if (selectedName != null && user.getName().equalsIgnoreCase(selectedName)) {
                    usersTable.getSelectionModel().select(user);
                }
            }
            if (me != null) {
                myCashLabel.setText(String.format("You: %s    Cash: %.2f%s",
                        me.getName(), me.getCash(), me.isBlocked() ? "    BLOCKED" : ""));
                if (me.getLedger() == null || me.getLedger().isEmpty()) {
                    ledgerArea.setText("No account lines yet.");
                } else {
                    ledgerArea.setText(String.join("\n", me.getLedger()));
                }
            }
        });
        task.setOnFailed(e -> {
            usersRequestRunning = false;
            if (showErrors) {
                showError("Users", task.getException().getMessage());
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onTopupClick(ActionEvent event) {
        if (Session.userName == null) {
            showError("Top up", "Login first.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(topupField.getText().trim());
        } catch (Exception e) {
            showError("Top up", "Amount must be a number.");
            return;
        }
        String body = "username=" + enc(Session.userName) + "&amount=" + amount;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.BASE + "/topup"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException(messageFrom(response.body()));
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> loadUsersFromServer());
        task.setOnFailed(e -> showError("Top up failed", task.getException().getMessage()));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
