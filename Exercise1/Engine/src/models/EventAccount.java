package models;

public class EventAccount{

    private double balance;
    private double totalCommissionCollected;

    public EventAccount(double balance) {
        this.balance = balance;
        this.totalCommissionCollected = 0.0;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalCommissionCollected() {
        return totalCommissionCollected;
    }

    public void addCommission(double amount){
        if(amount > 0){
            this.balance += amount;
            this.totalCommissionCollected += amount;
        }
    }

    public void addInvestment(double amount){
        if(amount > 0){
            this.balance += amount;
        }
    }

    public void deductFunds(double amount){
        if(amount > 0){
            this.balance -= amount;
        }
    }
}
