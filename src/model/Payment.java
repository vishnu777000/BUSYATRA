package model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Payment {

    private int id;
    private int ticketId;

    private double amount;
    private String status;

    private String paymentMethod;
    private String transactionId;

    private LocalDateTime createdAt;

    // Status constants
    public static final String STATUS_INIT = "INIT";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    // Payment methods
    public static final String METHOD_UPI = "UPI";
    public static final String METHOD_CARD = "CARD";
    public static final String METHOD_WALLET = "WALLET";

    /* ================= CONSTRUCTORS ================= */

    public Payment() {}

    public Payment(int ticketId, double amount, String method) {

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }

        this.ticketId = ticketId;
        this.amount = amount;
        this.paymentMethod = method;
        this.status = STATUS_INIT; // 🔥 FIXED
        this.transactionId = generateTxnId();
        this.createdAt = LocalDateTime.now();
    }

    public Payment(int id, int ticketId, double amount,
                   String status, String method,
                   String transactionId, LocalDateTime createdAt) {

        this.id = id;
        this.ticketId = ticketId;
        this.amount = amount;
        this.status = status;
        this.paymentMethod = method;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }

    /* ================= GETTERS & SETTERS ================= */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }

    public double getAmount() { return amount; }

    public void setAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        this.amount = amount;
    }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (!STATUS_INIT.equalsIgnoreCase(status) &&
            !STATUS_SUCCESS.equalsIgnoreCase(status) &&
            !STATUS_FAILED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid payment status");
        }
        this.status = status;
    }

    public String getPaymentMethod() { return paymentMethod; }

    public void setPaymentMethod(String paymentMethod) {
        if (!METHOD_UPI.equalsIgnoreCase(paymentMethod) &&
            !METHOD_CARD.equalsIgnoreCase(paymentMethod) &&
            !METHOD_WALLET.equalsIgnoreCase(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method");
        }
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() { return transactionId; }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /* ================= BUSINESS METHODS ================= */

    public void markSuccess() {
        this.status = STATUS_SUCCESS;
    }

    public void markFailed() {
        this.status = STATUS_FAILED;
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equalsIgnoreCase(status);
    }

    /* ================= TXN GENERATOR ================= */

    private String generateTxnId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /* ================= DEBUG ================= */

    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", ticketId=" + ticketId +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", method='" + paymentMethod + '\'' +
                ", txnId='" + transactionId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}