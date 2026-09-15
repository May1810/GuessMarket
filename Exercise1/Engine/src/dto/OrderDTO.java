package dto;

import java.util.Collections;
import java.util.List;

public class OrderDTO {
    private final String userName;
    private final String side;
    private final int quantity;
    private final double price;

    public OrderDTO(String userName, String side, int quantity, double price) {
        this.userName = userName;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
    }

    public String getUserName() {
        return userName;
    }

    public String getSide() {
        return side;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
