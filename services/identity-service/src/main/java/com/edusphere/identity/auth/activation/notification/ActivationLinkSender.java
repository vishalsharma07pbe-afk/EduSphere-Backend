package com.edusphere.identity.auth.activation.notification;

import com.edusphere.identity.user.entity.User;

public interface ActivationLinkSender {

    void sendActivationLink(
            User user,
            String rawToken
    );
}