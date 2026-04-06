package model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Schedule {

    private int id;

    private int busId;
    private int routeId;

    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    private String status;

    

    private String operator;
    private String busType;
    private String routeName;

    
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    

    public Schedule() {}

    public Schedule(int id, int busId, int routeId,
                    LocalDateTime departureTime,
                    LocalDateTime arrivalTime) {

        this.id = id;
        this.busId = busId;
        this.routeId = routeId;
        setDepartureTime(departureTime);
        setArrivalTime(arrivalTime);
        this.status = STATUS_ACTIVE;
    }

    

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBusId() { return busId; }
    public void setBusId(int busId) { this.busId = busId; }

    public int getRouteId() { return routeId; }
    public void setRouteId(int routeId) { this.routeId = routeId; }

    public LocalDateTime getDepartureTime() { return departureTime; }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public LocalDateTime getArrivalTime() { return arrivalTime; }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        if (departureTime != null && arrivalTime != null &&
            arrivalTime.isBefore(departureTime)) {
            throw new IllegalArgumentException("Arrival cannot be before departure");
        }
        this.arrivalTime = arrivalTime;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(status) &&
            !STATUS_INACTIVE.equalsIgnoreCase(status) &&
            !STATUS_CANCELLED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid schedule status");
        }
        this.status = status.toUpperCase();
    }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    

    public String getFormattedDeparture() {
        return departureTime != null ? departureTime.format(FORMATTER) : "";
    }

    public String getFormattedArrival() {
        return arrivalTime != null ? arrivalTime.format(FORMATTER) : "";
    }

    public long getDurationInMinutes() {
        if (departureTime != null && arrivalTime != null) {
            return Duration.between(departureTime, arrivalTime).toMinutes();
        }
        return 0;
    }

    

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public boolean isExpired() {
        return departureTime != null && departureTime.isBefore(LocalDateTime.now());
    }

    

    @Override
    public String toString() {
        return "Schedule{" +
                "id=" + id +
                ", busId=" + busId +
                ", routeId=" + routeId +
                ", departure=" + departureTime +
                ", arrival=" + arrivalTime +
                ", operator='" + operator + '\'' +
                ", route='" + routeName + '\'' +
                '}';
    }
}
