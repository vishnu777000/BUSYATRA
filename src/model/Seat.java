package model;

public class Seat {

    private int id;
    private int scheduleId;
    private String seatNo;

    private String status;
    private int userId;

    
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_BOOKED = "BOOKED";

    

    public Seat() {}

    public Seat(int scheduleId, String seatNo) {
        this.scheduleId = scheduleId;
        this.seatNo = seatNo;
        this.status = STATUS_AVAILABLE;
    }

    public Seat(int id, int scheduleId, String seatNo,
                String status, int userId) {

        this.id = id;
        this.scheduleId = scheduleId;
        this.seatNo = seatNo;
        setStatus(status);
        this.userId = userId;
    }

    

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getSeatNo() { return seatNo; }
    public void setSeatNo(String seatNo) {
        this.seatNo = seatNo;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (!STATUS_AVAILABLE.equalsIgnoreCase(status) &&
            !STATUS_LOCKED.equalsIgnoreCase(status) &&
            !STATUS_BOOKED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid seat status");
        }
        this.status = status.toUpperCase();
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    

    public boolean isAvailable() {
        return STATUS_AVAILABLE.equalsIgnoreCase(status);
    }

    public boolean isLocked() {
        return STATUS_LOCKED.equalsIgnoreCase(status);
    }

    public boolean isBooked() {
        return STATUS_BOOKED.equalsIgnoreCase(status);
    }

    

    
    public boolean lockSeat(int userId) {
        if (isAvailable()) {
            this.status = STATUS_LOCKED;
            this.userId = userId;
            return true;
        }
        return false;
    }

    
    public void releaseSeat() {
        if (isLocked()) {
            this.status = STATUS_AVAILABLE;
            this.userId = 0;
        }
    }

    
    public boolean bookSeat(int userId) {
        if (isLocked() && this.userId == userId) {
            this.status = STATUS_BOOKED;
            return true;
        }
        return false;
    }

    

    @Override
    public String toString() {
        return "Seat{" +
                "seatNo='" + seatNo + '\'' +
                ", status='" + status + '\'' +
                ", userId=" + userId +
                '}';
    }
}