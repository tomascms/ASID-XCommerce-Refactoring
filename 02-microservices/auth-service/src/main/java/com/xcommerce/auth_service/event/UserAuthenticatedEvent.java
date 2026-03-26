package com.xcommerce.auth_service.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserAuthenticatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long userId;
    private String email;
    private String authMethod;
    private String tokenType;
    private String ipAddress;
    private LocalDateTime authenticatedAt;
    
    public UserAuthenticatedEvent() {}
    
    public UserAuthenticatedEvent(Long userId, String email, String authMethod, String tokenType, String ipAddress) {
        this.userId = userId;
        this.email = email;
        this.authMethod = authMethod;
        this.tokenType = tokenType;
        this.ipAddress = ipAddress;
        this.authenticatedAt = LocalDateTime.now();
    }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAuthMethod() { return authMethod; }
    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public LocalDateTime getAuthenticatedAt() { return authenticatedAt; }
    public void setAuthenticatedAt(LocalDateTime authenticatedAt) { this.authenticatedAt = authenticatedAt; }
    
    @Override
    public String toString() {
        return "UserAuthenticatedEvent{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", authMethod='" + authMethod + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", authenticatedAt=" + authenticatedAt +
                '}';
    }
}
