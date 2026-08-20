package com.edusphere.school.school.service;

import com.edusphere.school.common.dto.PageResponse;
import com.edusphere.school.school.DTO.InitialAuthorityRequest;
import com.edusphere.school.school.DTO.SchoolOnboardingRequest;
import com.edusphere.school.school.DTO.SchoolProvisioningResponse;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.entity.School;
import com.edusphere.school.school.entity.SchoolProvisioning;
import com.edusphere.school.school.enums.ProvisioningStatus;
import com.edusphere.school.school.enums.SchoolStatus;
import com.edusphere.school.school.exception.DuplicateResourceException;
import com.edusphere.school.school.exception.InvalidRequestException;
import com.edusphere.school.school.exception.ResourceNotFoundException;
import com.edusphere.school.school.provisioning.IdentityInitialAuthorityRequest;
import com.edusphere.school.school.provisioning.IdentityProvisioningClient;
import com.edusphere.school.school.provisioning.IdentityProvisioningRequest;
import com.edusphere.school.school.mapper.SchoolMapper;
import com.edusphere.school.school.repository.SchoolProvisioningRepository;
import com.edusphere.school.school.repository.schoolRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;
import com.edusphere.school.school.provisioning.IdentityProvisioningResponse;
import java.util.List;
import org.springframework.data.domain.Sort;
import java.util.Set;

@Service
public class SchoolServiceImpl implements SchoolService {
    private static final Set<String> ALLOWED_SORT_FIELDS =
        Set.of(
                "name",
                "schoolCode",
                "createdAt",
                "updatedAt"
        );


    private final schoolRepository schoolRepository;
    private final SchoolProvisioningRepository provisioningRepository;
    private final SchoolMapper schoolMapper;
    private final IdentityProvisioningClient identityProvisioningClient;
    private final TransactionTemplate transactionTemplate;

    public SchoolServiceImpl(
            schoolRepository schoolRepository,
            SchoolProvisioningRepository provisioningRepository,
            SchoolMapper schoolMapper,
            IdentityProvisioningClient identityProvisioningClient,
            TransactionTemplate transactionTemplate
    ) {
        this.schoolRepository = schoolRepository;
        this.provisioningRepository = provisioningRepository;
        this.schoolMapper = schoolMapper;
        this.identityProvisioningClient = identityProvisioningClient;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse getSchoolById(long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(()->new ResourceNotFoundException("School not found"));
        return schoolMapper.toResponse(school);
    }

    @Override
    public SchoolProvisioningResponse onboardSchool(SchoolOnboardingRequest request) {
        Long schoolId = transactionTemplate.execute(status -> {
            if(schoolRepository.existsBySchoolCode(request.getSchoolCode())) {
                throw new DuplicateResourceException("School code already exists");
            }
            School savedSchool = schoolRepository.save(schoolMapper.toEntity(request));
            InitialAuthorityRequest authority = request.getInitialAuthority();
            provisioningRepository.save(new SchoolProvisioning(
                    savedSchool.getId(),
                    authority.getFirstName(),
                    authority.getMiddleName(),
                    authority.getLastName(),
                    authority.getUsername(),
                    authority.getEmail(),
                    authority.getPhone()
            ));
            return savedSchool.getId();
        });

        runProvisioningAttempt(schoolId);
        return getProvisioningStatus(schoolId);
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolProvisioningResponse getProvisioningStatus(long schoolId) {
        School school = findSchool(schoolId);
        SchoolProvisioning provisioning = provisioningRepository.findBySchoolId(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School provisioning not found"));
        return toProvisioningResponse(school, provisioning);
    }

    @Override
    public SchoolProvisioningResponse retryProvisioning(long schoolId) {
        SchoolProvisioning provisioning = provisioningRepository.findBySchoolId(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School provisioning not found"));
        if (provisioning.getStatus() != ProvisioningStatus.FAILED) {
            throw new InvalidRequestException("Only failed provisioning can be retried");
        }
        runProvisioningAttempt(schoolId);
        return getProvisioningStatus(schoolId);
    }

    @Override
    @Transactional
    public SchoolResponse updateSchool(long schoolId, UpdateSchoolRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(()->new ResourceNotFoundException("School not found"));
        schoolMapper.updateEntity(request, school);
        School updatedSchool = schoolRepository.save(school);
        return schoolMapper.toResponse(updatedSchool);
    }

    @Override
    @Transactional
    public void deleteSchool(long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(()->new ResourceNotFoundException("School not found"));
        school.deactivate();
        schoolRepository.save(school);
    }

    @Override
    @Transactional
    public SchoolResponse restoreSchool(long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(()->new ResourceNotFoundException("School not found"));
        SchoolProvisioning provisioning = provisioningRepository.findBySchoolId(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School provisioning not found"));
        if (provisioning.getStatus() != ProvisioningStatus.SUCCEEDED) {
            throw new InvalidRequestException("School identity provisioning has not succeeded");
        }
        school.activate();
        schoolRepository.save(school);
        return schoolMapper.toResponse(school);
    }

    private void runProvisioningAttempt(Long schoolId) {
        SchoolProvisioning provisioning =
                beginAttempt(schoolId);

        try {
            IdentityProvisioningResponse identityResponse =
                    identityProvisioningClient
                            .provisionInitialAuthority(
                                    toIdentityRequest(provisioning),
                                    idempotencyKey(provisioning)
                            );

            validateIdentityResponse(
                    schoolId,
                    identityResponse
            );

            completeAttempt(schoolId, null);
        } catch (RuntimeException exception) {
            completeAttempt(
                    schoolId,
                    safeErrorSummary(exception)
            );
        }
    }

    private SchoolProvisioning beginAttempt(Long schoolId) {
        return transactionTemplate.execute(status -> {
            School school = findSchool(schoolId);
            SchoolProvisioning provisioning = provisioningRepository.findWithLockBySchoolId(schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("School provisioning not found"));
            if (provisioning.getStatus() == ProvisioningStatus.SUCCEEDED) {
                throw new InvalidRequestException("School provisioning already succeeded");
            }
            if (provisioning.getStatus() == ProvisioningStatus.PENDING && provisioning.getAttemptCount() > 0) {
                throw new InvalidRequestException("School provisioning is already in progress");
            }
            provisioning.startAttempt();
            school.markProvisioningPending();
            return provisioning;
        });
    }

    private void completeAttempt(Long schoolId, String safeErrorSummary) {
        transactionTemplate.executeWithoutResult(status -> {
            School school = findSchool(schoolId);
            SchoolProvisioning provisioning = provisioningRepository.findWithLockBySchoolId(schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("School provisioning not found"));
            if (safeErrorSummary == null) {
                provisioning.succeed();
                school.activate();
            } else {
                provisioning.fail(safeErrorSummary);
                school.markProvisioningFailed();
            }
        });
    }

    private IdentityProvisioningRequest toIdentityRequest(SchoolProvisioning provisioning) {
        School school = findSchool(provisioning.getSchoolId());
        return new IdentityProvisioningRequest(
                provisioning.getSchoolId(),
                school.getSchoolCode(),
                school.getName(),
                school.getEmail(),
                new IdentityInitialAuthorityRequest(
                        provisioning.getAuthorityUsername(),
                        provisioning.getAuthorityFirstName(),
                        provisioning.getAuthorityMiddleName(),
                        provisioning.getAuthorityLastName(),
                        provisioning.getAuthorityEmail(),
                        provisioning.getAuthorityPhone()
                )
        );
    }

    private String idempotencyKey(SchoolProvisioning provisioning) {
        return "school-provisioning:" + provisioning.getSchoolId() + ":" + provisioning.getId();
    }

    private String safeErrorSummary(RuntimeException exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return "Identity provisioning failed with HTTP status "
                    + responseException.getStatusCode().value();
        }
        return "Identity provisioning failed: " + exception.getClass().getSimpleName();
    }

    private School findSchool(long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(()->new ResourceNotFoundException("School not found"));
    }

    private SchoolProvisioningResponse toProvisioningResponse(
            School school,
            SchoolProvisioning provisioning
    ) {
        return new SchoolProvisioningResponse(
                school.getId(),
                school.getStatus(),
                provisioning.getStatus(),
                provisioning.getAttemptCount(),
                provisioning.getLastErrorSummary(),
                provisioning.getCreatedAt(),
                provisioning.getUpdatedAt()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public PageResponse<SchoolResponse> getAllSchools(
            int page,
            int size,
            String sortBy,
            String direction,
            SchoolStatus status,
            String search
        ){
        if (page < 0) {
        throw new InvalidRequestException(
                "Page number cannot be negative"
        );
        }

        if (size < 1 || size > 100) {
            throw new InvalidRequestException(
                    "Page size must be between 1 and 100"
            );
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException(
                    "Invalid sort field: " + sortBy
            );
        }

        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException(
                    "Sort direction must be asc or desc"
            );
        }
        Sort sort = Sort.by(sortDirection, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        String normalizedSearch =
        search == null ? "" : search.trim();

        Page<School> schoolPage;

        if(normalizedSearch.isBlank()) {
            schoolPage = schoolRepository.findAllByStatus(status, pageable);
        } else {
            schoolPage = schoolRepository.searchByStatus(status, normalizedSearch, pageable);
        }

        List<SchoolResponse> schoolResponses =
                schoolPage.getContent()
                        .stream()
                        .map(schoolMapper::toResponse)
                        .toList();

        return new PageResponse<>(
                schoolResponses,
                schoolPage.getNumber(),
                schoolPage.getSize(),
                schoolPage.getTotalElements(),
                schoolPage.getTotalPages(),
                schoolPage.isFirst(),
                schoolPage.isLast()
        );
    }

    private void validateIdentityResponse(
            Long expectedSchoolId,
            IdentityProvisioningResponse response
    ) {
        if (!expectedSchoolId.equals(
                response.organizationId()
        )) {
            throw new IllegalStateException(
                    "Identity-service returned an unexpected organization ID"
            );
        }

        if (!"ACTIVE".equals(
                response.organizationStatus()
        )) {
            throw new IllegalStateException(
                    "Identity organization is not active"
            );
        }

        if (response.authorityUserId() == null) {
            throw new IllegalStateException(
                    "Identity-service did not return an authority user ID"
            );
        }

        if (!"PENDING_ACTIVATION".equals(
                response.authorityStatus()
        )) {
            throw new IllegalStateException(
                    "Initial authority has an unexpected status"
            );
        }

        if (!"SUCCEEDED".equals(
                response.provisioningStatus()
        )) {
            throw new IllegalStateException(
                    "Identity provisioning did not succeed"
            );
        }
    }
}
