package model;

public class Route {

    private int id;

    private String routeName;     // Vizag → Vijayawada
    private int totalDistance;    // in KM
    private double baseFare;      // per KM

    private String routeMap;
    private String status;

    // Status constants
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    /* ================= CONSTRUCTORS ================= */

    public Route() {}

    public Route(int id, String routeName,
                 int totalDistance, double baseFare) {

        this.id = id;
        this.routeName = routeName;
        setTotalDistance(totalDistance);
        setBaseFare(baseFare);
        this.status = STATUS_ACTIVE; // 🔥 default
    }

    public Route(int id, String routeName,
                 int totalDistance, double baseFare,
                 String routeMap, String status) {

        this.id = id;
        this.routeName = routeName;
        setTotalDistance(totalDistance);
        setBaseFare(baseFare);
        this.routeMap = routeMap;
        setStatus(status);
    }

    /* ================= GETTERS & SETTERS ================= */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRouteName() { return routeName; }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public int getTotalDistance() { return totalDistance; }

    public void setTotalDistance(int totalDistance) {
        if (totalDistance <= 0) {
            throw new IllegalArgumentException("Distance must be > 0");
        }
        this.totalDistance = totalDistance;
    }

    public double getBaseFare() { return baseFare; }

    public void setBaseFare(double baseFare) {
        if (baseFare < 0) {
            throw new IllegalArgumentException("Base fare cannot be negative");
        }
        this.baseFare = baseFare;
    }

    public String getRouteMap() { return routeMap; }

    public void setRouteMap(String routeMap) {
        this.routeMap = routeMap;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(status) &&
            !STATUS_INACTIVE.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid route status");
        }
        this.status = status.toUpperCase();
    }

    /* ================= HELPER ================= */

    public String getSource() {
        return extractPart(0);
    }

    public String getDestination() {
        return extractPart(1);
    }

    private String extractPart(int index) {
        if (routeName != null && routeName.contains("→")) {
            String[] parts = routeName.split("→");
            if (parts.length > index) {
                return parts[index].trim();
            }
        }
        return "";
    }

    /* ================= BUSINESS METHODS ================= */

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public void activate() {
        this.status = STATUS_ACTIVE;
    }

    public void deactivate() {
        this.status = STATUS_INACTIVE;
    }

    /* ================= DEBUG ================= */

    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", routeName='" + routeName + '\'' +
                ", distance=" + totalDistance +
                ", baseFare=" + baseFare +
                ", map='" + routeMap + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}