package models;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Event {

    private int id;
    private String name;
    private String description;
    private int commission;
    private String commissionType;
    private int b;

    private List<Trade> tradeHistory;
    private EventType type;
    private EventStatus status;
    private boolean allowMint;
    private double d;
    private int initialInvestment;
    private String winningOptionName;
    private String marketMakerName;

    private List<Option> options;
    private EventAccount account;
    private final Map<String, OptionBook> books = new LinkedHashMap<>();

    public Event(int id, String name, int commission, String description, String commissionType, int b, List<Option> options) {
        this(id, name, commission, description, commissionType, b, options, true);
    }

    public Event(int id, String name, int commission, String description, String commissionType, int b, List<Option> options, boolean startActive) {
        this.id = id;
        this.name = name;
        this.commission = commission;
        this.description = description;
        this.commissionType = commissionType;
        this.b = b;
        this.options = options;
        this.type = EventType.LMSR;
        this.tradeHistory = new ArrayList<>();
        this.winningOptionName = null;
        if (startActive) {
            this.status = EventStatus.ACTIVE;
            this.account = new EventAccount(this.b * Math.log(options.size()));
        } else {
            this.status = EventStatus.NOT_STARTED;
            this.account = new EventAccount(0.0);
        }
        initBooks();
    }

    public Event(int id, String name, List<Option> options, boolean allowMint, int initialInvestment, double d , int commission, String description, String commissionType){
        this.id = id;
        this.name = name;
        this.options = options;
        this.commission = commission;
        this.description = description;
        this.commissionType = commissionType;
        this.type = EventType.ORDER_BOOK;
        this.status = EventStatus.NOT_STARTED;
        this.account = new EventAccount(0.0);
        this.b = 0;
        this.allowMint = allowMint;
        this.initialInvestment = initialInvestment;
        this.d = d;
        this.tradeHistory = new ArrayList<>();
        this.winningOptionName = null;
        initBooks();
    }

    private void initBooks() {
        if (options == null) {
            return;
        }
        for (Option option : options) {
            books.put(option.getOptionName(), new OptionBook(option.getOptionName()));
        }
    }

    public void addOption(Option option) {
        if (this.options.size() < 2) {
            this.options.add(option);
            books.put(option.getOptionName(), new OptionBook(option.getOptionName()));
        }
    }

    public boolean isLmsr(){
        return this.type == EventType.LMSR;
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
        return status == EventStatus.ACTIVE;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setActive(boolean active) {
        this.status = active ? EventStatus.ACTIVE : EventStatus.CLOSED;
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

    public EventType getType() {
        return type;
    }

    public boolean isAllowMint() {
        return allowMint;
    }

    public double getD() {
        return d;
    }

    public int getInitialInvestment() {
        return initialInvestment;
    }

    public String getMarketMakerName() {
        return marketMakerName;
    }

    public void setMarketMakerName(String marketMakerName) {
        this.marketMakerName = marketMakerName;
    }

    public OptionBook bookOf(String optionName) {
        return books.get(optionName);
    }

    public Map<String, OptionBook> getBooks() {
        return books;
    }

    public double subsidyAmount() {
        return b * Math.log(options.size());
    }
}
