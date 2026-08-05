package com.edusphere.school.school.controller;

import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.exception.DuplicateResourceException;
import com.edusphere.school.school.exception.ResourceNotFoundException;
import com.edusphere.school.school.service.SchoolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchoolController.class)
class SchoolControllerTest {

    private static final String BASE_URL = "/api/v1/schools";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchoolService schoolService;

    @Test
    void createSchool_whenRequestIsValid_returnsCreatedSchool()
            throws Exception {

        String requestJson = """
                {
                  "schoolCode": "SCH001",
                  "name": "EduSphere Public School",
                  "email": "school@edusphere.com",
                  "phone": "9876543210",
                  "address": "New Delhi"
                }
                """;

        SchoolResponse response = createSchoolResponse(
                1L,
                "SCH001",
                "EduSphere Public School"
        );

        when(schoolService.createSchool(
                any(CreateSchoolRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.schoolCode").value("SCH001"))
                .andExpect(jsonPath("$.name")
                        .value("EduSphere Public School"))
                .andExpect(jsonPath("$.email")
                        .value("school@edusphere.com"))
                .andExpect(jsonPath("$.phone")
                        .value("9876543210"))
                .andExpect(jsonPath("$.address")
                        .value("New Delhi"));

        verify(schoolService)
                .createSchool(any(CreateSchoolRequest.class));
    }

    @Test
    void createSchool_whenRequestIsInvalid_returnsBadRequest()
            throws Exception {

        String requestJson = """
            {
              "schoolCode": "",
              "name": "",
              "email": "invalid-email",
              "phone": "123",
              "address": "New Delhi"
            }
            """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"));

        verifyNoInteractions(schoolService);
    }

    @Test
    void createSchool_whenCodeAlreadyExists_returnsConflict()
            throws Exception {

        String requestJson = """
                {
                  "schoolCode": "SCH001",
                  "name": "EduSphere Public School",
                  "email": "school@edusphere.com",
                  "phone": "9876543210",
                  "address": "New Delhi"
                }
                """;

        when(schoolService.createSchool(
                any(CreateSchoolRequest.class)
        )).thenThrow(
                new DuplicateResourceException(
                        "School code already exists"
                )
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("School code already exists"))
                .andExpect(jsonPath("$.path")
                        .value(BASE_URL));

        verify(schoolService)
                .createSchool(any(CreateSchoolRequest.class));
    }

    @Test
    void getSchool_whenSchoolExists_returnsSchoolResponse()
            throws Exception {

        long schoolId = 1L;

        when(schoolService.getSchoolById(schoolId))
                .thenReturn(createSchoolResponse(
                        schoolId,
                        "SCH001",
                        "EduSphere Public School"
                ));

        mockMvc.perform(get(
                        BASE_URL + "/{schoolId}",
                        schoolId
                ))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.schoolCode").value("SCH001"))
                .andExpect(jsonPath("$.name")
                        .value("EduSphere Public School"));

        verify(schoolService).getSchoolById(schoolId);
    }

    @Test
    void getSchool_whenSchoolDoesNotExist_returnsNotFound()
            throws Exception {

        long schoolId = 99L;

        when(schoolService.getSchoolById(schoolId))
                .thenThrow(
                        new ResourceNotFoundException(
                                "School not found"
                        )
                );

        mockMvc.perform(get(
                        BASE_URL + "/{schoolId}",
                        schoolId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("School not found"))
                .andExpect(jsonPath("$.path")
                        .value(BASE_URL + "/99"));

        verify(schoolService).getSchoolById(schoolId);
    }

    @Test
    void getAllSchools_whenSchoolsExist_returnsSchoolResponses()
            throws Exception {

        SchoolResponse firstSchool = createSchoolResponse(
                1L,
                "SCH001",
                "EduSphere Public School"
        );

        SchoolResponse secondSchool = createSchoolResponse(
                2L,
                "SCH002",
                "EduSphere International School"
        );

        when(schoolService.getAllSchools())
                .thenReturn(List.of(firstSchool, secondSchool));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].schoolCode")
                        .value("SCH001"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].schoolCode")
                        .value("SCH002"));

        verify(schoolService).getAllSchools();
    }

    @Test
    void getAllSchools_whenNoSchoolsExist_returnsEmptyList()
            throws Exception {

        when(schoolService.getAllSchools())
                .thenReturn(List.of());

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(schoolService).getAllSchools();
    }

    @Test
    void updateSchool_whenRequestIsValid_returnsUpdatedSchool()
            throws Exception {

        long schoolId = 1L;

        String requestJson = """
                {
                  "name": "Updated EduSphere School",
                  "email": "updated@edusphere.com",
                  "phone": "9876543211",
                  "address": "Gurugram"
                }
                """;

        SchoolResponse response = createSchoolResponse(
                schoolId,
                "SCH001",
                "Updated EduSphere School"
        );

        when(schoolService.updateSchool(
                eq(schoolId),
                any(UpdateSchoolRequest.class)
        )).thenReturn(response);

        mockMvc.perform(put(
                        BASE_URL + "/{schoolId}",
                        schoolId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.schoolCode").value("SCH001"))
                .andExpect(jsonPath("$.name")
                        .value("Updated EduSphere School"));

        verify(schoolService).updateSchool(
                eq(schoolId),
                any(UpdateSchoolRequest.class)
        );
    }

    @Test
    void updateSchool_whenRequestIsInvalid_returnsBadRequest()
            throws Exception {

        String requestJson = """
                {
                  "schoolCode": "",
                  "name": "",
                  "email": "invalid-email",
                  "phone": "123",
                  "address": "New Delhi"
                }
                """;

        mockMvc.perform(put(
                        BASE_URL + "/{schoolId}",
                        1L
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"));

        verifyNoInteractions(schoolService);
    }

    @Test
    void updateSchool_whenSchoolDoesNotExist_returnsNotFound()
            throws Exception {

        long schoolId = 99L;

        String requestJson = """
                {
                  "name": "Updated School",
                  "email": "updated@edusphere.com",
                  "phone": "9876543210",
                  "address": "New Delhi"
                }
                """;

        when(schoolService.updateSchool(
                eq(schoolId),
                any(UpdateSchoolRequest.class)
        )).thenThrow(
                new ResourceNotFoundException("School not found")
        );

        mockMvc.perform(put(
                        BASE_URL + "/{schoolId}",
                        schoolId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("School not found"))
                .andExpect(jsonPath("$.path")
                        .value(BASE_URL + "/99"));

        verify(schoolService).updateSchool(
                eq(schoolId),
                any(UpdateSchoolRequest.class)
        );
    }

    @Test
    void deleteSchool_whenSchoolExists_returnsNoContent()
            throws Exception {

        mockMvc.perform(delete(
                        BASE_URL + "/{schoolId}",
                        1L
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(schoolService).deleteSchool(1L);
    }

    @Test
    void deleteSchool_whenSchoolDoesNotExist_returnsNotFound()
            throws Exception {

        long schoolId = 99L;

        org.mockito.Mockito.doThrow(
                new ResourceNotFoundException("School not found")
        ).when(schoolService).deleteSchool(schoolId);

        mockMvc.perform(delete(
                        BASE_URL + "/{schoolId}",
                        schoolId
                ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("School not found"))
                .andExpect(jsonPath("$.path")
                        .value(BASE_URL + "/99"));

        verify(schoolService).deleteSchool(schoolId);
    }

    private SchoolResponse createSchoolResponse(
            long id,
            String schoolCode,
            String name
    ) {
        return new SchoolResponse(
                id,
                schoolCode,
                name,
                "school@edusphere.com",
                "9876543210",
                "New Delhi",
                null,
                null
        );
    }
}