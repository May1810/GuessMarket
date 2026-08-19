package dto;

public class BuyResultDTO {
    private double basePrice;
    private double commission;
    private double totalPaid;

    public BuyResultDTO(double basePrice, double commission) {
        this.basePrice = basePrice;
        this.commission = commission;
        this.totalPaid = basePrice + commission; // חישוב אוטומטי בבנאי
    }

    public double getBasePrice() { return basePrice; }
    public double getCommission() { return commission; }
    public double getTotalPaid() { return totalPaid; }
}