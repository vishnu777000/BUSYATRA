package model;

import java.time.LocalDateTime;

public class User {

    private int id;
    private String name;
    private String email;
    private String password;

    private String role;
    private String status;

    private LocalDateTime createdAt;

    // Status constants
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BLOCKED = "BLOCKED";

    /* ================= CONSTRUCTORS ================= */

    public User() {}

    public User(int id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        setRole(role);
        this.status = STATUS_ACTIVE;
    }

    public User(int id, String name, String email,
                String role, String status,
                LocalDateTime createdAt) {

        this.id = id;
        this.name = name;
        this.email = email;
        setRole(role);
        setStatus(status);
        this.createdAt = createdAt;
    }

    /* ================= GETTERS & SETTERS ================= */

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }

    public void setEmail(String email) {
        this.email = email.toLowerCase(); // 🔥 normalize
    }

    public String getPassword() { return password; }

    public void setPassword(String password) {
        // 🔥 Assume already hashed before setting
        this.password = password;
    }

    public String getRole() { return role; }

    public void setRole(String role) {
        if (!Role.isValidRole(role)) {
            throw new IllegalArgumentException("Invalid role");
        }
        this.role = role.toUpperCase();
    }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (!STATUS_ACTIVE.equalsIgnoreCase(status) &&
            !STATUS_BLOCKED.equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Invalid user status");
        }
        this.status = status.toUpperCase();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /* ================= HELPERS ================= */

    public boolean isAdmin() {
        return Role.isAdmin(role);
    }

    public boolean isUser() {
        return Role.isUser(role);
    }

    public boolean isManager() {
        return Role.isManager(role);
    }

    public boolean isClerk() {
        return Role.isClerk(role);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    /* ================= BUSINESS METHODS ================= */

    public void blockUser() {
        this.status = STATUS_BLOCKED;
    }

    public void activateUser() {
        this.status = STATUS_ACTIVE;
    }

    /* ================= DEBUG ================= */

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}