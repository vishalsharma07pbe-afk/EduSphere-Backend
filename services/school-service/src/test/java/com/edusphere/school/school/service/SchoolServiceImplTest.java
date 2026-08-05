package com.edusphere.school.school.service;

import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.entity.School;
import com.edusphere.school.school.exception.DuplicateResourceException;
import com.edusphere.school.school.exception.ResourceNotFoundException;
import com.edusphere.school.school.mapper.SchoolMapper;
import com.edusphere.school.school.repository.schoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.edusphere.school.school.enums.SchoolStatus.INACTIVE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolServiceImplTest {

    @Mock
    private schoolRepository schoolRepository;

    @Mock
    private SchoolMapper schoolMapper;

    @InjectMocks
    private SchoolServiceImpl schoolService;

    @Test
    void getSchoolById_whenSchoolExists_returnsSchoolResponse() {
        // Arrange
        long schoolId = 1L;

        School school = mock(School.class);
        SchoolResponse expectedResponse = mock(SchoolResponse.class);

        when(schoolRepository.findById(schoolId))
                .thenReturn(Optional.of(school));

        when(schoolMapper.toResponse(school))
                .thenReturn(expectedResponse);

        // Act
        SchoolResponse actualResponse =
                schoolService.getSchoolById(schoolId);

        // Assert
        assertSame(expectedResponse, actualResponse);

        verify(schoolRepository).findById(schoolId);
        verify(schoolMapper).toResponse(school);
    }

    @Test
    void getSchoolById_whenSchoolDoesNotExist_throwsResourceNotFoundException() {
        long schoolId = 1L;
        when(schoolRepository.findById(schoolId))
                .thenReturn(Optional.empty());

        //Act and Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> schoolService.getSchoolById(schoolId)
        );

        assertSame("School not found", exception.getMessage());

        verify(schoolRepository).findById(schoolId);
        verifyNoInteractions(schoolMapper);
    }

    @Test
    void createSchool_whenCodeIsUnique_createAndReturnsSchoolResponse() {
        CreateSchoolRequest request = mock(CreateSchoolRequest.class);
        School school = mock(School.class);
        School savedSchool = mock(School.class);
        SchoolResponse expectedResponse = mock(SchoolResponse.class);

        when(request.getSchoolCode()).thenReturn("schC001");
        when(schoolRepository.existsBySchoolCode("schC001")).thenReturn(false);
        when(schoolMapper.toEntity(request)).thenReturn(school);
        when(schoolRepository.save(school)).thenReturn(savedSchool);
        when(schoolMapper.toResponse(savedSchool)).thenReturn(expectedResponse);
        //Act
        SchoolResponse actualResponse =
                schoolService.createSchool(request);
        //Assert
        assertSame(expectedResponse, actualResponse);

        verify(schoolRepository).existsBySchoolCode("schC001");
        verify(schoolMapper).toEntity(request);
        verify(schoolRepository).save(school);
        verify(schoolMapper).toResponse(savedSchool);
    }

    @Test
    void createSchool_whenCodeIsNotUnique_throwsDuplicateCodeException() {
        CreateSchoolRequest request = mock(CreateSchoolRequest.class);

        when(request.getSchoolCode()).thenReturn("schC001");
        when(schoolRepository.existsBySchoolCode("schC001")).thenReturn(true);
        //Act and Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> schoolService.createSchool(request)
        );
        assertEquals("School code already exists", exception.getMessage());

        verify(schoolRepository).existsBySchoolCode("schC001");
        verifyNoInteractions(schoolMapper);
        verify(schoolRepository,never()).save(any(School.class));
    }

    @Test
    void updateSchool_whenSchoolExists_returnsSchoolResponse() {
        Long id = 1L;
        UpdateSchoolRequest request = mock(UpdateSchoolRequest.class);
        School existingschool = mock(School.class);
        School updatedSchool = mock(School.class);
        SchoolResponse expectedResponse = mock(SchoolResponse.class);

        when(schoolRepository.findById(id)).thenReturn(Optional.of(existingschool));
        when(schoolRepository.save(existingschool)).thenReturn(updatedSchool);
        when(schoolMapper.toResponse(updatedSchool)).thenReturn(expectedResponse);

        SchoolResponse actualResponse = schoolService.updateSchool(id, request);
        assertSame(expectedResponse, actualResponse);

        verify(schoolRepository).findById(id);
        verify(schoolMapper).updateEntity(request, existingschool);
        verify(schoolRepository).save(existingschool);
        verify(schoolMapper).toResponse(updatedSchool);
    }

    @Test
    void updateSchool_whenSchoolDoesNotExist_throwsResourceNotFoundException() {
        long id = 99L;
        UpdateSchoolRequest request = mock(UpdateSchoolRequest.class);

        when(schoolRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                ()->schoolService.updateSchool(id, request));

        assertEquals("School not found", exception.getMessage());

        verify(schoolRepository).findById(id);
        verifyNoInteractions(schoolMapper);
        verify(schoolRepository,never()).save(any(School.class));
    }

    @Test
    void deleteSchool_whenSchoolExists_returnsSchoolResponse() {
        Long id = 1L;
        School school = mock(School.class);

        when(schoolRepository.findById(id)).thenReturn(Optional.of(school));
        when(schoolRepository.save(school)).thenReturn(school);

        schoolService.deleteSchool(id);

        verify(schoolRepository).findById(id);
        verify(school).setStatus(INACTIVE);
        verify(schoolRepository).save(school);
    }

    @Test
    void deleteSchool_whenSchoolDoesNotExist_throwsResourceNotFoundException() {
        Long id = 99L;
        when(schoolRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                ()->schoolService.deleteSchool(id));

        assertEquals("School not found", exception.getMessage());

        verify(schoolRepository).findById(id);
        verify(schoolRepository,never()).save(any(School.class));
    }

    @Test
    void getAllSchools_whenSchoolExists_returnsSchoolResponse() {
        School school1 = mock(School.class);
        School school2 = mock(School.class);
        SchoolResponse response1 = mock(SchoolResponse.class);
        SchoolResponse response2 = mock(SchoolResponse.class);

        when(schoolRepository.findAll()).thenReturn(List.of(school1,school2));
        when(schoolMapper.toResponse(school1)).thenReturn(response1);
        when(schoolMapper.toResponse(school2)).thenReturn(response2);

        List<SchoolResponse> actualResponse = schoolService.getAllSchools();
        assertEquals(List.of(response1, response2), actualResponse);

        verify(schoolRepository).findAll();
        verify(schoolMapper).toResponse(school1);
        verify(schoolMapper).toResponse(school2);
    }

    @Test
    void getAllSchools_whenSchoolDoesNotExist_returnsEmptyList() {
        when(schoolRepository.findAll()).thenReturn(List.of());

        List<SchoolResponse> actualResponse = schoolService.getAllSchools();
        assertTrue(actualResponse.isEmpty());

        verify(schoolRepository).findAll();
        verifyNoInteractions(schoolMapper);
    }
}