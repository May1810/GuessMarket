package models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class User {
    private final String name;
    private double cash;
    private double reservedCash;
    private boolean blocked;
    private final Set<Integer> mmEventIds = new HashSet<>();
    private final Map<Integer, UserPosition> positions = new HashMap<>();

    public User(String name, double cash) {
        this.name = name;
        this.cash = cash;
    }

    public String getName() {
        return name;
    }

    public double getCash() {
        return cash;
    }

    public double getReservedCash() {
        return reservedCash;
    }

    public double getDisplayedCash() {
        return cash + reservedCash;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Set<Integer> getMmEventIds() {
        return mmEventIds;
    }

    public boolean isMarketMakerOf(int eventId) {
        return mmEventIds.contains(eventId);
    }

    public Map<Integer, UserPosition> getPositions() {
        return positions;
    }

    public UserPosition position(int eventId) {
        return positions.computeIfAbsent(eventId, id -> new UserPosition());
    }

    public boolean hasParticipated(int eventId) {
        return positions.containsKey(eventId);
    }

    public void requireNotBlocked() {
        if (blocked) {
            throw new IllegalArgumentException("User '" + name + "' is blocked (negative balance) and cannot act.");
        }
    }

    public void debit(double amount) {
        if (amount <= 0) {
            return;
        }
        cash -= amount;
        if (cash < 0) {
            blocked = true;
        }
    }

    public void credit(double amount) {
        if (amount <= 0) {
            return;
        }
        cash += amount;
        if (cash < 0) {
            blocked = true;
        }
    }

    public void requireFunds(double amount) {
        if (amount > cash + 1e-9) {
            throw new IllegalArgumentException(
                    "User '" + name + "' does not have enough cash. Needed: "
                            + String.format("%.2f", amount) + ", available: "
                            + String.format("%.2f", cash));
        }
    }

    public void reserveCash(double amount) {
        requireFunds(amount);
        cash -= amount;
        reservedCash += amount;
    }

    public void releaseCash(double amount) {
        if (amount <= 0) {
            return;
        }
        double release = Math.min(amount, reservedCash);
        reservedCash -= release;
        cash += release;
    }

    public void consumeReserved(double amount) {
        if (amount <= 0) {
            return;
        }
        reservedCash -= amount;
        if (reservedCash < 0) {
            cash += reservedCash;
            reservedCash = 0;
            if (cash < 0) {
                blocked = true;
            }
        }
    }
}
