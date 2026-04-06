package model;

import java.time.LocalDateTime;

public class AuditLog {

    private int id;
    private int userId;
    private String action;
    private String description;
    private LocalDateTime actionTime;

    

    public AuditLog() {}

    public AuditLog(int userId, String action, String description) {
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.actionTime = LocalDateTime.now();
    }

    public AuditLog(int id, int userId, String action,
                    String description, LocalDateTime actionTime) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.description = description;
        this.actionTime = actionTime;
    }

    

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    

    public void setId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }

    

    
    public String[] toRow() {
        return new String[]{
                String.valueOf(id),
                String.valueOf(userId),
                action,
                description,
                actionTime.toString()
        };
    }

    

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", userId=" + userId +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                ", actionTime=" + actionTime +
                '}';
    }
}