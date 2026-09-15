package models;

public class BookOrder {
    private final long seq;
    private final String userName;
    private final boolean buy;
    private int remaining;
    private final double price;
    private double reservedCash;

    public BookOrder(long seq, String userName, boolean buy, int remaining, double price, double reservedCash) {
        this.seq = seq;
        this.userName = userName;
        this.buy = buy;
        this.remaining = remaining;
        this.price = price;
        this.reservedCash = reservedCash;
    }

    public long getSeq() {
        return seq;
    }

    public String getUserName() {
        return userName;
    }

    public boolean isBuy() {
        return buy;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    public double getPrice() {
        return price;
    }

    public double getReservedCash() {
        return reservedCash;
    }

    public void setReservedCash(double reservedCash) {
        this.reservedCash = reservedCash;
    }
}
