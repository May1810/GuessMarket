package dto;

public class OptionDTO {
    private final String optionName;
    private final int shareBought;
    private double currentValue;

    public OptionDTO(String optionName, int shareBought, double currentValue) {
        this.optionName = optionName;
        this.shareBought = shareBought;
        this.currentValue = currentValue; 
    }

    public String getOptionName() { return optionName; }

    public int getShareBought() { return shareBought; }
    
    public double getCurrentValue() { return currentValue; }
}
