import dto.BuyResultDTO;
import dto.EventDTO;
import dto.OptionDTO;
import dto.OrderDTO;
import dto.ParticipantDTO;
import dto.UserDTO;
import dto.UserEventDTO;
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
import javafx.util.StringConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    private final IGuessMarketEngine engine = new GuessMarketEngine();

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
    @FXML private TableColumn<UserDTO, String> userBlockedColumn;
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
                showEventDetails(engine.getEventById(selected.getId()));
            }
        });

        userNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        userCashColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                String.format("%.2f", cell.getValue().getCash())));
        userBlockedColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
                cell.getValue().isBlocked() ? "Yes" : "No"));

        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                showUserDetails(engine.getUserByName(selected.getName()));
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
                Thread.sleep(1500);
                engine.loadXmlFile(file.getAbsolutePath());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            loadedPathLabel.setText(file.getAbsolutePath());
            tradeResultLabel.setText("");
            refreshAll(null, null);
            finishLoadUi();
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
        UserDTO user = selectedUser();
        EventDTO selected = tradeEventCombo.getValue();
        if (user == null || selected == null) {
            showError("Open event", "Select a user and an event.");
            return;
        }
        try {
            engine.openEvent(user.getName(), selected.getId());
            tradeResultLabel.setText("Event opened.");
            refreshAll(user.getName(), selected.getId());
        } catch (Exception e) {
            showError("Open failed", e.getMessage());
        }
    }

    @FXML
    private void onCloseClick(ActionEvent event) {
        UserDTO user = selectedUser();
        EventDTO selected = tradeEventCombo.getValue();
        String winner = winnerCombo.getValue();
        if (user == null || selected == null) {
            showError("Close event", "Select a user and an event.");
            return;
        }
        if (winner == null || winner.isBlank()) {
            showError("Close event", "Select the winning option.");
            return;
        }
        try {
            engine.closeEvent(user.getName(), selected.getId(), winner);
            tradeResultLabel.setText("Event closed. Winner: " + winner);
            refreshAll(user.getName(), selected.getId());
        } catch (Exception e) {
            showError("Close failed", e.getMessage());
        }
    }

    @FXML
    private void onBuyClick(ActionEvent event) {
        UserDTO user = selectedUser();
        EventDTO selected = tradeEventCombo.getValue();
        String optionName = optionCombo.getValue();
        if (user == null || selected == null) {
            showError("Buy", "Select a user and an event.");
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
        try {
            BuyResultDTO receipt = engine.buyShares(user.getName(), selected.getId(), optionName, amount);
            tradeResultLabel.setText(String.format(
                    "Paid total %.2f (shares %.2f + commission %.2f)",
                    receipt.getTotalPaid(), receipt.getBasePrice(), receipt.getCommission()));
            if (receipt.getMessage() != null) {
                showError("Notice", receipt.getMessage());
            }
            refreshAll(user.getName(), selected.getId());
        } catch (Exception e) {
            showError("Buy failed", e.getMessage());
        }
    }

    @FXML
    private void onPlaceOrderClick(ActionEvent event) {
        UserDTO user = selectedUser();
        EventDTO selected = tradeEventCombo.getValue();
        String optionName = optionCombo.getValue();
        String side = sideCombo.getValue();
        if (user == null || selected == null) {
            showError("Order", "Select a user and an event.");
            return;
        }
        if (optionName == null || optionName.isBlank()) {
            showError("Order", "Select an option.");
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
        try {
            BuyResultDTO result = engine.placeOrder(user.getName(), selected.getId(), optionName, side, qty, price);
            tradeResultLabel.setText(result.getMessage() == null ? "Order processed." : result.getMessage());
            if (result.getMessage() != null && result.getMessage().contains("blocked")) {
                showError("Notice", result.getMessage());
            }
            refreshAll(user.getName(), selected.getId());
        } catch (Exception e) {
            showError("Order failed", e.getMessage());
        }
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

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
