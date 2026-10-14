package dto;

import java.util.List;

public class UserDTO {
    private final String name;
    private final double cash;
    private final boolean blocked;
    private final List<Integer> mmEventIds;
    private final List<UserEventDTO> participations;
    private final List<String> ledger;

    public UserDTO(String name, double cash, boolean blocked, List<Integer> mmEventIds, List<UserEventDTO> participations, List<String> ledger) {
        this.name = name;
        this.cash = cash;
        this.blocked = blocked;
        this.mmEventIds = mmEventIds;
        this.participations = participations;
        this.ledger = ledger;
    }

    public String getName() {
        return name;
    }

    public double getCash() {
        return cash;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public List<Integer> getMmEventIds() {
        return mmEventIds;
    }

    public List<UserEventDTO> getParticipations() {
        return participations;
    }

    public List<String> getLedger() {
        return ledger;
    }

    public boolean isMarketMaker() {
        return mmEventIds != null && !mmEventIds.isEmpty();
    }
}
