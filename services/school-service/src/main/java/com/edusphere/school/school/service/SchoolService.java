package com.edusphere.school.school.service;

import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;

import java.util.List;

public interface SchoolService {
    SchoolResponse getSchoolById(long schoolId);
    SchoolResponse createSchool(CreateSchoolRequest request);
    SchoolResponse updateSchool(long schoolId, UpdateSchoolRequest request);
    void deleteSchool(long schoolId);
    List<SchoolResponse> getAllSchools();
}
