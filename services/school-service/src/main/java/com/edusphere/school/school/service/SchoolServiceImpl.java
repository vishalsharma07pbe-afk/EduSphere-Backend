package com.edusphere.school.school.service;

import com.edusphere.school.common.dto.PageResponse;
import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.entity.School;
import com.edusphere.school.school.enums.SchoolStatus;
import com.edusphere.school.school.exception.DuplicateResourceException;
import com.edusphere.school.school.exception.ResourceNotFoundException;
import com.edusphere.school.school.mapper.SchoolMapper;
import com.edusphere.school.school.repository.schoolRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.edusphere.school.school.enums.SchoolStatus.INACTIVE;
import static com.edusphere.school.school.enums.SchoolStatus.ACTIVE;
import org.springframework.data.domain.Sort;
import com.edusphere.school.school.exception.InvalidRequestException;
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
    private final SchoolMapper schoolMapper;

    public SchoolServiceImpl(schoolRepository schoolRepository, SchoolMapper schoolMapper) {
        this.schoolRepository = schoolRepository;
        this.schoolMapper = schoolMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolResponse getSchoolById(long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(()->new ResourceNotFoundException("School not found"));
        return schoolMapper.toResponse(school);
    }

    @Override
    @Transactional
    public SchoolResponse createSchool(CreateSchoolRequest request) {
        if(schoolRepository.existsBySchoolCode(request.getSchoolCode())) {
            throw new DuplicateResourceException("School code already exists");
        }
        School school = schoolMapper.toEntity(request);
        School savedSchool = schoolRepository.save(school);
        return schoolMapper.toResponse(savedSchool);
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
        school.setStatus(INACTIVE);
        schoolRepository.save(school);
    }

    @Override
    @Transactional
    public SchoolResponse restoreSchool(long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(()->new ResourceNotFoundException("School not found"));
        school.setStatus(ACTIVE);
        schoolRepository.save(school);
        return schoolMapper.toResponse(school);
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
}
