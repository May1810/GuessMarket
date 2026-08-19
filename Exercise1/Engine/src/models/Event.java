package models;
import java.util.ArrayList;
import java.util.List;

public class Event {

    private int id;
    private String name;
    private String description;
    private int commission;
    private String commissionType;
    private int b;

    private List<Trade> tradeHistory;
    private String winningOptionName;

    private boolean isActive;

    private List<Option> options;
    private EventAccount account;

    public Event(int id, String name, int commission, String description, String commissionType, int b, List<Option> options) {
        this.id = id;
        this.name = name;
        this.commission = commission;
        this.description = description;
        this.commissionType = commissionType;
        this.b = b;

        this.isActive = true;
        this.options = options;

       this.tradeHistory = new ArrayList<>();
       this.winningOptionName = null;
       double initialBalance = this.b * Math.log(options.size());
       this.account = new EventAccount(initialBalance);

    }

    public void addOption(Option option) {
        if (this.options.size() < 2) {
            this.options.add(option);
        }
    }

    public EventAccount getAccount() {
        return account;
    }

    public void setAccount(EventAccount account) {
        this.account = account;
    }

    public int getId() {
        return id;
    }

    public String getCommissionType() {
        return commissionType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCommission() {
        return commission;
    }

    public int getB() {
        return b;
    }

    public boolean isActive() {
        return isActive;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void addTrade(Trade trade) {
        this.tradeHistory.add(0, trade);
    }

    public List<Trade> getTradeHistory() {
        return tradeHistory;
    }

    public String getWinningOptionName() {
        return winningOptionName;
    }

    public void setWinningOptionName(String winningOptionName) {
        this.winningOptionName = winningOptionName;
    }

}
