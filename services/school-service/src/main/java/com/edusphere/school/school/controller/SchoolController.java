package com.edusphere.school.school.controller;

import com.edusphere.school.common.dto.PageResponse;
import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.enums.SchoolStatus;
import com.edusphere.school.school.service.SchoolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schools")
public class SchoolController {
    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @PostMapping
    public ResponseEntity<SchoolResponse> createSchool(
            @Valid @RequestBody CreateSchoolRequest request) {
        SchoolResponse response = schoolService.createSchool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{schoolId}")
    public ResponseEntity<SchoolResponse> getSchool(
            @PathVariable Long schoolId){
        SchoolResponse response = schoolService.getSchoolById(schoolId);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<SchoolResponse>> getAllSchools(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "ACTIVE") SchoolStatus status,
            @RequestParam(defaultValue = "") String search
    ) {
        PageResponse<SchoolResponse> response =
                schoolService.getAllSchools(page, size, sortBy, direction, status, search);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{schoolId}")
    public ResponseEntity<SchoolResponse> updateSchool(
            @PathVariable Long schoolId, @Valid @RequestBody UpdateSchoolRequest request){
        SchoolResponse response = schoolService.updateSchool(schoolId, request);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/{schoolId}")
    public ResponseEntity<Void> deleteSchool(@PathVariable Long schoolId){
        schoolService.deleteSchool(schoolId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{schoolId}/restore")
    public ResponseEntity<SchoolResponse> restoreSchool(@PathVariable Long schoolId){
        SchoolResponse response = schoolService.restoreSchool(schoolId);
        return ResponseEntity.ok().body(response);
    }
}
