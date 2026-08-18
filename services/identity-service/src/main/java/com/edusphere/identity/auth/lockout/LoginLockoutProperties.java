package com.edusphere.identity.auth.lockout;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "security.login-lockout")
public class LoginLockoutProperties {

    private int firstFailureThreshold = 5;
    private int escalatedFailureThreshold = 3;
    private Duration firstLockDuration = Duration.ofMinutes(30);
    private Duration secondLockDuration = Duration.ofDays(30);
    private Duration finalLockDuration = Duration.ofDays(365);
    private String lockedMessagePrefix = "Account is locked until ";
    private String lockedMessageSuffix =
            ". Please contact an admin to unlock sooner.";

    public int getFirstFailureThreshold() {
        return firstFailureThreshold;
    }

    public void setFirstFailureThreshold(int firstFailureThreshold) {
        this.firstFailureThreshold = firstFailureThreshold;
    }

    public int getEscalatedFailureThreshold() {
        return escalatedFailureThreshold;
    }

    public void setEscalatedFailureThreshold(
            int escalatedFailureThreshold
    ) {
        this.escalatedFailureThreshold = escalatedFailureThreshold;
    }

    public Duration getFirstLockDuration() {
        return firstLockDuration;
    }

    public void setFirstLockDuration(Duration firstLockDuration) {
        this.firstLockDuration = firstLockDuration;
    }

    public Duration getSecondLockDuration() {
        return secondLockDuration;
    }

    public void setSecondLockDuration(Duration secondLockDuration) {
        this.secondLockDuration = secondLockDuration;
    }

    public Duration getFinalLockDuration() {
        return finalLockDuration;
    }

    public void setFinalLockDuration(Duration finalLockDuration) {
        this.finalLockDuration = finalLockDuration;
    }

    public String getLockedMessagePrefix() {
        return lockedMessagePrefix;
    }

    public void setLockedMessagePrefix(String lockedMessagePrefix) {
        this.lockedMessagePrefix = lockedMessagePrefix;
    }

    public String getLockedMessageSuffix() {
        return lockedMessageSuffix;
    }

    public void setLockedMessageSuffix(String lockedMessageSuffix) {
        this.lockedMessageSuffix = lockedMessageSuffix;
    }
}
