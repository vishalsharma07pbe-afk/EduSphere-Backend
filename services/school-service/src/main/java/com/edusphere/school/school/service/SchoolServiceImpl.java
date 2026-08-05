package com.edusphere.school.school.service;

import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.entity.School;
import com.edusphere.school.school.exception.DuplicateResourceException;
import com.edusphere.school.school.exception.ResourceNotFoundException;
import com.edusphere.school.school.mapper.SchoolMapper;
import com.edusphere.school.school.repository.schoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.edusphere.school.school.enums.SchoolStatus.INACTIVE;

@Service
public class SchoolServiceImpl implements SchoolService {

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
    @Transactional(readOnly = true)
    public List<SchoolResponse> getAllSchools() {
        return schoolRepository.findAll()
                .stream()
                .map(schoolMapper::toResponse)
                .toList();
    }
}
