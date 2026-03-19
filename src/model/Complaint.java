package model;

import java.time.LocalDateTime;

public class Complaint {

    private int id;
    private int userId;
    private String category;
    private String message;

    private String status;
    private String adminReply;

    private LocalDateTime createdAt;

    // Status constants
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RESOLVED = "RESOLVED";

    /* ================= CONSTRUCTORS ================= */

    public Complaint() {}

    public Complaint(int userId, String category, String message) {
        this.userId = userId;
        this.category = category;
        this.message = message;
        this.status = STATUS_OPEN; // 🔥 fixed
        this.createdAt = LocalDateTime.now();
    }

    public Complaint(int id, int userId, String category,
                     String message, String status,
                     String adminReply, LocalDateTime createdAt) {

        this.id = id;
        this.userId = userId;
        this.category = category;
        this.message = message;
        this.status = status;
        this.adminReply = adminReply;
        this.createdAt = createdAt;
    }

    /* ================= GETTERS & SETTERS ================= */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (!STATUS_OPEN.equalsIgnoreCase(status) &&
            !STATUS_RESOLVED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid status");
        }
        this.status = status;
    }

    public String getAdminReply() { return adminReply; }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;

        // 🔥 auto resolve when admin replies
        if (adminReply != null && !adminReply.trim().isEmpty()) {
            this.status = STATUS_RESOLVED;
        }
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /* ================= BUSINESS METHODS ================= */

    public boolean isOpen() {
        return STATUS_OPEN.equalsIgnoreCase(status);
    }

    public boolean isResolved() {
        return STATUS_RESOLVED.equalsIgnoreCase(status);
    }

    public void markResolved(String reply) {
        this.adminReply = reply;
        this.status = STATUS_RESOLVED;
    }

    /* ================= DEBUG ================= */

    @Override
    public String toString() {
        return "Complaint{" +
                "id=" + id +
                ", userId=" + userId +
                ", category='" + category + '\'' +
                ", message='" + message + '\'' +
                ", status='" + status + '\'' +
                ", adminReply='" + adminReply + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}