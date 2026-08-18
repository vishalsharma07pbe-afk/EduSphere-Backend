package com.edusphere.identity.auth.refreshtoken.model;

import java.time.Duration;

public record RefreshTokenSessionLifetime(
        Duration inactivityExpiration,
        Duration absoluteSessionLifetime
) {
}
