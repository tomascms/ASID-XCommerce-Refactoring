package com.xcommerce.user_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserCreatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String status;
    private LocalDateTime createdAt;
    
    public UserCreatedEvent() {}
    
    public UserCreatedEvent(Long userId, String email, String firstName, String lastName, String phoneNumber) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
    }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "UserCreatedEvent{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
