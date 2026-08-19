package dto;

import java.util.List;

public class EventDTO {
    private int id;
    private String name;
    private boolean isActive;
    private List<OptionDTO> options;

    private int commission;
    private String commissionType;
    private double accountBalance;
    private double totalCommissionCollected;
    private List<TradeDTO> tradeHistory;
    private String winningOptionName; 
    
    private String description;


    public EventDTO(int id, String name, String description, boolean isActive, 
                    List<OptionDTO> options, int commission, String commissionType, 
                    double accountBalance, double totalCommissionCollected, 
                    List<TradeDTO> tradeHistory, String winningOptionName) {
        this.id = id;
        this.name = name;
        this.description = description; // <-- השמה
        this.isActive = isActive;
        this.options = options;
        this.commission = commission;
        this.commissionType = commissionType;
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.tradeHistory = tradeHistory;
        this.winningOptionName = winningOptionName;
    }

    public String getDescription() { return description; }
    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public List<OptionDTO> getOptions() { return options; }
    public int getCommission() { return commission; }
    public String getCommissionType() { return commissionType; }
    public double getAccountBalance() { return accountBalance; }
    public double getTotalCommissionCollected() { return totalCommissionCollected; }
    public List<TradeDTO> getTradeHistory() { return tradeHistory; }
    public String getWinningOptionName() { return winningOptionName; }

}