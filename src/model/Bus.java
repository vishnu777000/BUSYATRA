package model;

public class Bus {

    private int id;
    private String operator;
    private String busType;
    private int totalSeats;
    private double fare;
    private double fareMultiplier;
    private String busNumber;
    private String status;

    

    public Bus() {}

    public Bus(int id, String operator, String busType,
               int totalSeats, double fare) {

        this.id = id;
        this.operator = operator;
        this.busType = busType;
        this.totalSeats = totalSeats;
        this.fare = fare;
    }

    public Bus(int id, String operator, String busType,
               int totalSeats, double fare,
               double fareMultiplier, String busNumber, String status) {

        this.id = id;
        this.operator = operator;
        this.busType = busType;
        this.totalSeats = totalSeats;
        this.fare = fare;
        this.fareMultiplier = fareMultiplier;
        this.busNumber = busNumber;
        this.status = status;
    }

    

    public int getId() { return id; }

    public String getOperator() { return operator; }

    public String getBusType() { return busType; }

    public int getTotalSeats() { return totalSeats; }

    public double getFare() { return fare; }

    public double getFareMultiplier() { return fareMultiplier; }

    public String getBusNumber() { return busNumber; }

    public String getStatus() { return status; }

    

    public void setId(int id) { this.id = id; }

    public void setOperator(String operator) { this.operator = operator; }

    public void setBusType(String busType) { this.busType = busType; }

    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    public void setFare(double fare) { this.fare = fare; }

    public void setFareMultiplier(double fareMultiplier) { this.fareMultiplier = fareMultiplier; }

    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public void setStatus(String status) { this.status = status; }

    

    
    public String[] toRow() {
        return new String[]{
                String.valueOf(id),
                operator,
                busType,
                String.valueOf(totalSeats),
                String.valueOf(fare),
                status
        };
    }

    
    public String getDisplayName() {
        return operator + " (" + busType + ")";
    }

    

    @Override
    public String toString() {
        return "Bus{" +
                "id=" + id +
                ", operator='" + operator + '\'' +
                ", busType='" + busType + '\'' +
                ", totalSeats=" + totalSeats +
                ", fare=" + fare +
                ", fareMultiplier=" + fareMultiplier +
                ", busNumber='" + busNumber + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}