package model;

import java.time.LocalDateTime;

public class Cancellation {

    private int id;
    private int ticketId;
    private int userId;
    private String reason;

    private double refundAmount;
    private String status;

    private LocalDateTime cancelledAt;

    
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REFUNDED = "REFUNDED";

    

    public Cancellation() {}

    public Cancellation(int ticketId, int userId,
                        String reason, double refundAmount) {

        this.ticketId = ticketId;
        this.userId = userId;
        this.reason = reason;
        this.refundAmount = refundAmount;
        this.status = STATUS_PENDING; 
        this.cancelledAt = LocalDateTime.now();
    }

    public Cancellation(int id, int ticketId, int userId,
                        String reason, double refundAmount,
                        String status, LocalDateTime cancelledAt) {

        this.id = id;
        this.ticketId = ticketId;
        this.userId = userId;
        this.reason = reason;
        this.refundAmount = refundAmount;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) {
        if (refundAmount < 0) {
            throw new IllegalArgumentException("Refund cannot be negative");
        }
        this.refundAmount = refundAmount;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    

    public boolean isRefunded() {
        return STATUS_REFUNDED.equalsIgnoreCase(status);
    }

    public void markRefunded() {
        this.status = STATUS_REFUNDED;
    }

    

    @Override
    public String toString() {
        return "Cancellation{" +
                "id=" + id +
                ", ticketId=" + ticketId +
                ", userId=" + userId +
                ", reason='" + reason + '\'' +
                ", refund=" + refundAmount +
                ", status='" + status + '\'' +
                ", cancelledAt=" + cancelledAt +
                '}';
    }
}