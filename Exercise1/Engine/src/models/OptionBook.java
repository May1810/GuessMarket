package models;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OptionBook {
    private final String optionName;
    private Double lastPrice;
    private final List<BookOrder> bids = new ArrayList<>();
    private final List<BookOrder> asks = new ArrayList<>();

    public OptionBook(String optionName) {
        this.optionName = optionName;
    }

    public String getOptionName() {
        return optionName;
    }

    public Double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public List<BookOrder> getBids() {
        return bids;
    }

    public List<BookOrder> getAsks() {
        return asks;
    }

    public void add(BookOrder order) {
        if (order.getRemaining() <= 0) {
            return;
        }
        if (order.isBuy()) {
            bids.add(order);
            sortBids();
        } else {
            asks.add(order);
            sortAsks();
        }
    }

    public void remove(BookOrder order) {
        bids.remove(order);
        asks.remove(order);
    }

    public void sortBids() {
        bids.sort(Comparator
                .comparingDouble(BookOrder::getPrice).reversed()
                .thenComparingLong(BookOrder::getSeq));
    }

    public void sortAsks() {
        asks.sort(Comparator
                .comparingDouble(BookOrder::getPrice)
                .thenComparingLong(BookOrder::getSeq));
    }

    public BookOrder bestBid(String excludeUser) {
        for (BookOrder order : bids) {
            if (excludeUser == null || !order.getUserName().equalsIgnoreCase(excludeUser)) {
                return order;
            }
        }
        return null;
    }

    public BookOrder bestAsk(String excludeUser) {
        for (BookOrder order : asks) {
            if (excludeUser == null || !order.getUserName().equalsIgnoreCase(excludeUser)) {
                return order;
            }
        }
        return null;
    }

    public Double bestBidPrice() {
        return bids.isEmpty() ? null : bids.get(0).getPrice();
    }

    public Double bestAskPrice() {
        return asks.isEmpty() ? null : asks.get(0).getPrice();
    }

    public Double mid() {
        Double bid = bestBidPrice();
        Double ask = bestAskPrice();
        if (bid == null || ask == null) {
            return null;
        }
        return (bid + ask) / 2.0;
    }

    public Double spread() {
        Double bid = bestBidPrice();
        Double ask = bestAskPrice();
        if (bid == null || ask == null) {
            return null;
        }
        return ask - bid;
    }
}
