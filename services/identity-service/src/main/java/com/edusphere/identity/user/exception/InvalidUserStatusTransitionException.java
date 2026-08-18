package com.edusphere.identity.user.exception;

public class InvalidUserStatusTransitionException extends RuntimeException {
    public InvalidUserStatusTransitionException(String message) {
        super(message);
    }
}
