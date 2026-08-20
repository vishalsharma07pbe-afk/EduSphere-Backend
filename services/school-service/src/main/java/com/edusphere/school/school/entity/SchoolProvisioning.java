package com.edusphere.school.school.entity;

import com.edusphere.school.school.enums.ProvisioningStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "school_provisioning",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_school_provisioning_school",
                        columnNames = "school_id"
                )
        }
)
public class SchoolProvisioning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(
            name = "authority_first_name",
            nullable = false,
            length = 100
    )
    private String authorityFirstName;

    @Column(
            name = "authority_middle_name",
            length = 100
    )
    private String authorityMiddleName;

    @Column(
            name = "authority_last_name",
            length = 100
    )
    private String authorityLastName;

    @Column(name = "authority_username", nullable = false, length = 100)
    private String authorityUsername;

    @Column(name = "authority_email", nullable = false, length = 150)
    private String authorityEmail;

    @Column(name = "authority_phone", nullable = false, length = 20)
    private String authorityPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProvisioningStatus status = ProvisioningStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "last_error_summary", length = 500)
    private String lastErrorSummary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public SchoolProvisioning() {
    }

    public SchoolProvisioning(
            Long schoolId,
            String authorityFirstName,
            String authorityMiddleName,
            String authorityLastName,
            String authorityUsername,
            String authorityEmail,
            String authorityPhone
    ) {
        this.schoolId = schoolId;
        this.authorityFirstName = authorityFirstName;
        this.authorityMiddleName = authorityMiddleName;
        this.authorityLastName = authorityLastName;
        this.authorityUsername = authorityUsername;
        this.authorityEmail = authorityEmail;
        this.authorityPhone = authorityPhone;
        this.status = ProvisioningStatus.PENDING;
    }

    public Long getId() { return id; }
    public Long getVersion() { return version; }
    public Long getSchoolId() { return schoolId; }
    public String getAuthorityFirstName() {
        return authorityFirstName;
    }
    public String getAuthorityMiddleName() {
        return authorityMiddleName;
    }
    public String getAuthorityLastName() {
        return authorityLastName;
    }
    public String getAuthorityUsername() { return authorityUsername; }
    public String getAuthorityEmail() { return authorityEmail; }
    public String getAuthorityPhone() { return authorityPhone; }
    public ProvisioningStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public String getLastErrorSummary() { return lastErrorSummary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void startAttempt() {
        this.attemptCount++;
        this.lastErrorSummary = null;
        this.status = ProvisioningStatus.PENDING;
    }

    public void succeed() {
        this.status = ProvisioningStatus.SUCCEEDED;
        this.lastErrorSummary = null;
    }

    public void fail(String safeErrorSummary) {
        this.status = ProvisioningStatus.FAILED;
        this.lastErrorSummary = safeErrorSummary;
    }
}
