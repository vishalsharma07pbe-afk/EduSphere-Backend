package com.edusphere.identity.auth.passwordreset.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "security.password-reset-token")
public class PasswordResetTokenProperties {

    private Duration expiration = Duration.ofHours(1);
    private Duration requestWindow = Duration.ofHours(24);
    private Duration resetWindow = Duration.ofHours(24);
    private int maxEmailsPerWindow = 3;
    private int maxResetsPerWindow = 1;

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public Duration getRequestWindow() {
        return requestWindow;
    }

    public void setRequestWindow(Duration requestWindow) {
        this.requestWindow = requestWindow;
    }

    public int getMaxEmailsPerWindow() {
        return maxEmailsPerWindow;
    }

    public void setMaxEmailsPerWindow(int maxEmailsPerWindow) {
        this.maxEmailsPerWindow = maxEmailsPerWindow;
    }

    public Duration getResetWindow() {
        return resetWindow;
    }

    public void setResetWindow(Duration resetWindow) {
        this.resetWindow = resetWindow;
    }

    public int getMaxResetsPerWindow() {
        return maxResetsPerWindow;
    }

    public void setMaxResetsPerWindow(int maxResetsPerWindow) {
        this.maxResetsPerWindow = maxResetsPerWindow;
    }
}
