package models;

public class Option {
    private String  optionName;
    private int shareBought;

    public Option(String optionName) {
        this.optionName = optionName;
        this.shareBought = 0;
    }

    public int getShareBought() {
        return shareBought;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setShareBought(int shareBought) {
        this.shareBought = shareBought;
    }
}
