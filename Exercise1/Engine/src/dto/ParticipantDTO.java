package dto;

public class ParticipantDTO {
    private final String name;
    private final String holdingsSummary;
    private final double totalCommission;

    public ParticipantDTO(String name, String holdingsSummary, double totalCommission) {
        this.name = name;
        this.holdingsSummary = holdingsSummary;
        this.totalCommission = totalCommission;
    }

    public String getName() {
        return name;
    }

    public String getHoldingsSummary() {
        return holdingsSummary;
    }

    public double getTotalCommission() {
        return totalCommission;
    }
}
