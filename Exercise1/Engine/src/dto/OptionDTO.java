package dto;

import java.util.Collections;
import java.util.List;

public class OptionDTO {
    private final String optionName;
    private final int shareBought;
    private double currentValue;
    private final Double lastPrice;
    private final Double bidPrice;
    private final Double askPrice;
    private final Double midPrice;
    private final Double spread;
    private final List<OrderDTO> bids;
    private final List<OrderDTO> asks;

    public OptionDTO(String optionName, int shareBought, double currentValue) {
        this(optionName, shareBought, currentValue, null, null, null, null, null,
                Collections.emptyList(), Collections.emptyList());
    }

    public OptionDTO(String optionName, int shareBought, double currentValue,
                     Double lastPrice, Double bidPrice, Double askPrice, Double midPrice, Double spread,
                     List<OrderDTO> bids, List<OrderDTO> asks) {
        this.optionName = optionName;
        this.shareBought = shareBought;
        this.currentValue = currentValue;
        this.lastPrice = lastPrice;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.midPrice = midPrice;
        this.spread = spread;
        this.bids = bids;
        this.asks = asks;
    }

    public String getOptionName() { return optionName; }
    public int getShareBought() { return shareBought; }
    public double getCurrentValue() { return currentValue; }
    public Double getLastPrice() { return lastPrice; }
    public Double getBidPrice() { return bidPrice; }
    public Double getAskPrice() { return askPrice; }
    public Double getMidPrice() { return midPrice; }
    public Double getSpread() { return spread; }
    public List<OrderDTO> getBids() { return bids; }
    public List<OrderDTO> getAsks() { return asks; }
}
