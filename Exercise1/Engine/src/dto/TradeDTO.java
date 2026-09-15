package dto;

public class TradeDTO {
    private final String userName;
    private final String optionName;
    private final int quantity;
    private final double pricePaid;
    private final double commission;

    public TradeDTO(String optionName, int quantity, double pricePaid) {
        this(null, optionName, quantity, pricePaid, 0.0);
    }

    public TradeDTO(String userName, String optionName, int quantity, double pricePaid, double commission) {
        this.userName = userName;
        this.optionName = optionName;
        this.quantity = quantity;
        this.pricePaid = pricePaid;
        this.commission = commission;
    }

    public String getUserName() { return userName; }
    public String getOptionName() { return optionName; }
    public int getQuantity() { return quantity; }
    public double getPricePaid() { return pricePaid; }
    public double getCommission() { return commission; }
}
