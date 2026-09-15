package dto;

public class BuyResultDTO {
    private double basePrice;
    private double commission;
    private double totalPaid;
    private String message;

    public BuyResultDTO(double basePrice, double commission) {
        this(basePrice, commission, null);
    }

    public BuyResultDTO(double basePrice, double commission, String message) {
        this.basePrice = basePrice;
        this.commission = commission;
        this.totalPaid = basePrice + commission;
        this.message = message;
    }

    public double getBasePrice() { return basePrice; }
    public double getCommission() { return commission; }
    public double getTotalPaid() { return totalPaid; }
    public String getMessage() { return message; }
}
