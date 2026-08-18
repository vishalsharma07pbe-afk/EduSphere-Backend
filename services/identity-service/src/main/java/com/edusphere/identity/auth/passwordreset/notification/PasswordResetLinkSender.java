package com.edusphere.identity.auth.passwordreset.notification;

import com.edusphere.identity.user.entity.User;

public interface PasswordResetLinkSender {

    void sendPasswordResetLink(User user, String rawToken);
}
