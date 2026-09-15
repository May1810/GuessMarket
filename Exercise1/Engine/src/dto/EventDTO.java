package dto;

import java.util.Collections;
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
    private String status;
    private String methodType;
    private String marketMakerName;
    private List<ParticipantDTO> participants;
    private boolean allowMint;
    private double d;
    private int initialInvestment;

    public EventDTO(int id, String name, String description, boolean isActive,
                    List<OptionDTO> options, int commission, String commissionType,
                    double accountBalance, double totalCommissionCollected,
                    List<TradeDTO> tradeHistory, String winningOptionName) {
        this(id, name, description, isActive, options, commission, commissionType,
                accountBalance, totalCommissionCollected, tradeHistory, winningOptionName,
                isActive ? "Active" : "Closed", "LMSR", null, Collections.emptyList(), false, 0, 0);
    }

    public EventDTO(int id, String name, String description, boolean isActive,
                    List<OptionDTO> options, int commission, String commissionType,
                    double accountBalance, double totalCommissionCollected,
                    List<TradeDTO> tradeHistory, String winningOptionName,
                    String status, String methodType, String marketMakerName,
                    List<ParticipantDTO> participants, boolean allowMint, double d, int initialInvestment) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
        this.options = options;
        this.commission = commission;
        this.commissionType = commissionType;
        this.accountBalance = accountBalance;
        this.totalCommissionCollected = totalCommissionCollected;
        this.tradeHistory = tradeHistory;
        this.winningOptionName = winningOptionName;
        this.status = status;
        this.methodType = methodType;
        this.marketMakerName = marketMakerName;
        this.participants = participants;
        this.allowMint = allowMint;
        this.d = d;
        this.initialInvestment = initialInvestment;
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
    public String getStatus() { return status; }
    public String getMethodType() { return methodType; }
    public String getMarketMakerName() { return marketMakerName; }
    public List<ParticipantDTO> getParticipants() { return participants; }
    public boolean isAllowMint() { return allowMint; }
    public double getD() { return d; }
    public int getInitialInvestment() { return initialInvestment; }
}
