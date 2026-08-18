package com.edusphere.identity.auth.activation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "security.activation-token")
public class ActivationTokenProperties {

    private Duration expiration = Duration.ofHours(24);
    private Duration resendWindow = Duration.ofHours(24);
    private int maxEmailsPerWindow = 3;

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public Duration getResendWindow() {
        return resendWindow;
    }

    public void setResendWindow(Duration resendWindow) {
        this.resendWindow = resendWindow;
    }

    public int getMaxEmailsPerWindow() {
        return maxEmailsPerWindow;
    }

    public void setMaxEmailsPerWindow(
            int maxEmailsPerWindow
    ) {
        this.maxEmailsPerWindow = maxEmailsPerWindow;
    }
}