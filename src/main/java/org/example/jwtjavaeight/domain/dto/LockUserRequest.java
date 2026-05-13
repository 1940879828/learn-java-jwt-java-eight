package org.example.jwtjavaeight.domain.dto;

import javax.validation.constraints.Size;

public class LockUserRequest {
    @Size(max = 255)
    private String reason;

    // Getters and Setters
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
