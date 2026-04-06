package model;

import java.time.LocalDateTime;

public class Ticket {

    private int id;
    private int userId;
    private int scheduleId;

    private String seats;
    private double amount;

    private String status;
    private String passengerName;

    private LocalDateTime bookingTime;

    

    private String routeName;
    private String operator;
    private String busType;

    private String departureTime;
    private String arrivalTime;

    
    public static final String STATUS_BOOKED = "BOOKED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    

    public Ticket() {}

    public Ticket(int userId, int scheduleId,
                  String seats, double amount) {

        if (seats == null || seats.trim().isEmpty()) {
            throw new IllegalArgumentException("Seats cannot be empty");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        this.userId = userId;
        this.scheduleId = scheduleId;
        this.seats = seats;
        this.amount = amount;
        this.status = STATUS_BOOKED;
        this.bookingTime = LocalDateTime.now();
    }

    

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public String getSeats() { return seats; }

    public void setSeats(String seats) {
        if (seats == null || seats.trim().isEmpty()) {
            throw new IllegalArgumentException("Seats cannot be empty");
        }
        this.seats = seats;
    }

    public double getAmount() { return amount; }

    public void setAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        this.amount = amount;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (!STATUS_BOOKED.equalsIgnoreCase(status) &&
            !STATUS_CANCELLED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid ticket status");
        }
        this.status = status.toUpperCase();
    }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getBusType() { return busType; }
    public void setBusType(String busType) { this.busType = busType; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    

    public String getFormattedSeats() {
        return seats != null ? seats.replace(",", ", ") : "";
    }

    

    public boolean isBooked() {
        return STATUS_BOOKED.equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    public void cancelTicket() {
        this.status = STATUS_CANCELLED;
    }

    

    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", user=" + userId +
                ", route='" + routeName + '\'' +
                ", seats='" + seats + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                '}';
    }
}