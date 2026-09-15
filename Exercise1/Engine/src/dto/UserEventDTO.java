package dto;

public class UserEventDTO {
    private final int eventId;
    private final String eventName;
    private final String methodType;
    private final String involvement;
    private final String details;

    public UserEventDTO(int eventId, String eventName, String methodType, String involvement, String details) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.methodType = methodType;
        this.involvement = involvement;
        this.details = details;
    }

    public int getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public String getMethodType() {
        return methodType;
    }

    public String getInvolvement() {
        return involvement;
    }

    public String getDetails() {
        return details;
    }
}
