package models;

import java.util.HashMap;
import java.util.Map;

public class UserPosition {
    private final Map<String, Integer> shares = new HashMap<>();
    private final Map<String, Integer> reservedShares = new HashMap<>();
    private double totalPaid;
    private double totalCommission;
    private Double closedPnl;

    public int getShares(String optionName) {
        return shares.getOrDefault(optionName, 0);
    }

    public int getReservedShares(String optionName) {
        return reservedShares.getOrDefault(optionName, 0);
    }

    public int getAvailableShares(String optionName) {
        return getShares(optionName) - getReservedShares(optionName);
    }

    public void addShares(String optionName, int amount) {
        shares.put(optionName, getShares(optionName) + amount);
    }

    public void removeShares(String optionName, int amount) {
        shares.put(optionName, getShares(optionName) - amount);
    }

    public void addReservedShares(String optionName, int amount) {
        reservedShares.put(optionName, getReservedShares(optionName) + amount);
    }

    public void removeReservedShares(String optionName, int amount) {
        reservedShares.put(optionName, Math.max(0, getReservedShares(optionName) - amount));
    }

    public Map<String, Integer> getShares() {
        return shares;
    }

    public double getTotalPaid() {
        return totalPaid;
    }

    public void addPaid(double amount) {
        this.totalPaid += amount;
    }

    public double getTotalCommission() {
        return totalCommission;
    }

    public void addCommission(double amount) {
        this.totalCommission += amount;
    }

    public Double getClosedPnl() {
        return closedPnl;
    }

    public void setClosedPnl(double closedPnl) {
        this.closedPnl = closedPnl;
    }
}
