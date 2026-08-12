package com.edusphere.identity.user.dto;

import com.edusphere.identity.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateUserStatusRequest {

    @NotNull(message = "User status is required")
    private UserStatus status;

    public UpdateUserStatusRequest() {
    }

    public UpdateUserStatusRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}