package com.edusphere.school.school.service;

import com.edusphere.school.common.dto.PageResponse;
import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.enums.SchoolStatus;

public interface SchoolService {
    SchoolResponse getSchoolById(long schoolId);
    SchoolResponse createSchool(CreateSchoolRequest request);
    SchoolResponse updateSchool(long schoolId, UpdateSchoolRequest request);
    void deleteSchool(long schoolId);
    SchoolResponse restoreSchool(long schoolId);
    PageResponse<SchoolResponse> getAllSchools(
        int page, 
        int size,
        String sortBy, 
        String direction, 
        SchoolStatus status,
        String search);
}
