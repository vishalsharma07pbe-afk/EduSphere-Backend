package com.edusphere.identity;

import com.edusphere.identity.auth.activation.repository.UserActivationTokenRepository;
import com.edusphere.identity.auth.passwordreset.repository.UserPasswordResetTokenRepository;
import com.edusphere.identity.auth.refreshtoken.repository.RefreshTokenRepository;
import com.edusphere.identity.roleapproval.repository.RoleAssignmentApprovalRepository;
import com.edusphere.identity.roleapproval.repository.RoleAssignmentRequestRepository;
import com.edusphere.identity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
class IdentityServiceApplicationTests {

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private RoleAssignmentRequestRepository requestRepository;
    @MockitoBean
    private RoleAssignmentApprovalRepository approvalRepository;
    @MockitoBean
    private UserActivationTokenRepository activationTokenRepository;
    @MockitoBean
    private UserPasswordResetTokenRepository passwordResetTokenRepository;
    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;
    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void contextLoads() {
    }

}
