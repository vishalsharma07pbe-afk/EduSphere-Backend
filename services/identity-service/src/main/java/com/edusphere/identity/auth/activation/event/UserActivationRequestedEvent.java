package com.edusphere.identity.auth.activation.event;

public record UserActivationRequestedEvent(
        Long userId
) {
}