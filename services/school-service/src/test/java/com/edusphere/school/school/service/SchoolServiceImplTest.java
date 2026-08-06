package com.edusphere.school.school.service;

import com.edusphere.school.common.dto.PageResponse;
import com.edusphere.school.school.DTO.CreateSchoolRequest;
import com.edusphere.school.school.DTO.SchoolResponse;
import com.edusphere.school.school.DTO.UpdateSchoolRequest;
import com.edusphere.school.school.entity.School;
import com.edusphere.school.school.exception.DuplicateResourceException;
import com.edusphere.school.school.exception.ResourceNotFoundException;
import com.edusphere.school.school.exception.InvalidRequestException;
import com.edusphere.school.school.mapper.SchoolMapper;
import com.edusphere.school.school.repository.schoolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import com.edusphere.school.school.enums.SchoolStatus;
import static com.edusphere.school.school.enums.SchoolStatus.ACTIVE;
import static com.edusphere.school.school.enums.SchoolStatus.INACTIVE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    void restoreSchool_whenSchoolExists_returnsSchoolResponse() {
        Long id = 1L;
        School school = mock(School.class);
        SchoolResponse expectedResponse = mock(SchoolResponse.class);

        when(schoolRepository.findById(id)).thenReturn(Optional.of(school));
        when(schoolRepository.save(school)).thenReturn(school);
        when(schoolMapper.toResponse(school)).thenReturn(expectedResponse);

        SchoolResponse actualResponse = schoolService.restoreSchool(id);

        assertSame(expectedResponse, actualResponse);
        verify(schoolRepository).findById(id);
        verify(school).setStatus(ACTIVE);
        verify(schoolRepository).save(school);
        verify(schoolMapper).toResponse(school);
    }

    @Test
    void restoreSchool_whenSchoolDoesNotExist_throwsResourceNotFoundException() {
        Long id = 99L;
        when(schoolRepository.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                ()->schoolService.restoreSchool(id));

        assertEquals("School not found", exception.getMessage());
        verify(schoolRepository).findById(id);
        verify(schoolRepository,never()).save(any(School.class));
        verifyNoInteractions(schoolMapper);
    }

    @Test
    void getAllSchools_whenSchoolsExist_returnsSortedPageResponse() {
        // Arrange
        int page = 0;
        int size = 10;
        String sortBy = "name";
        String direction = "asc";

        Sort sort = Sort.by(Sort.Direction.ASC, sortBy);

        Pageable pageable =
                PageRequest.of(page, size, sort);

        School school1 = mock(School.class);
        School school2 = mock(School.class);

        SchoolResponse response1 = mock(SchoolResponse.class);
        SchoolResponse response2 = mock(SchoolResponse.class);

        Page<School> schoolPage = new PageImpl<>(
                List.of(school1, school2),
                pageable,
                2
        );

        SchoolStatus status = SchoolStatus.ACTIVE;

        when(schoolRepository.findAllByStatus(status, pageable))
                .thenReturn(schoolPage);

        when(schoolMapper.toResponse(school1))
                .thenReturn(response1);

        when(schoolMapper.toResponse(school2))
                .thenReturn(response2);

        // Act
        PageResponse<SchoolResponse> actualResponse =
                schoolService.getAllSchools(
                        page,
                        size,
                        sortBy,
                        direction,
                        status,
                        ""
                );

        // Assert
        assertEquals(
                List.of(response1, response2),
                actualResponse.content()
        );

        assertEquals(0, actualResponse.page());
        assertEquals(10, actualResponse.size());
        assertEquals(2, actualResponse.totalElements());
        assertEquals(1, actualResponse.totalPages());
        assertTrue(actualResponse.first());
        assertTrue(actualResponse.last());

        verify(schoolRepository).findAllByStatus(status, pageable);
        verify(schoolMapper).toResponse(school1);
        verify(schoolMapper).toResponse(school2);
  }

    @Test
    void getAllSchools_whenSearchIsProvided_returnsFilteredPage() {
        // Arrange
        int page = 0;
        int size = 10;
        String sortBy = "name";
        String direction = "desc";

        Sort sort = Sort.by(Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        School school = mock(School.class);
        SchoolResponse response = mock(SchoolResponse.class);

        Page<School> schoolPage = new PageImpl<>(
                List.of(school),
                pageable,
                1
        );

        SchoolStatus status = SchoolStatus.ACTIVE;
        String search = "public";

        when(schoolRepository.searchByStatus(status, search, pageable))
                .thenReturn(schoolPage);
        when(schoolMapper.toResponse(school)).thenReturn(response);

        // Act
        PageResponse<SchoolResponse> actualResponse =
                schoolService.getAllSchools(
                        page,
                        size,
                        sortBy,
                        direction,
                        status,
                        search
                );

        // Assert
        assertEquals(List.of(response), actualResponse.content());
        assertEquals(1, actualResponse.totalElements());
        assertEquals(1, actualResponse.totalPages());
        assertTrue(actualResponse.first());
        assertTrue(actualResponse.last());

        verify(schoolRepository).searchByStatus(status, search, pageable);
        verify(schoolMapper).toResponse(school);
    }

    @Test
    void getAllSchools_whenNoSchoolsExist_returnsEmptySortedPage() {
        // Arrange
        int page = 0;
        int size = 10;
        String sortBy = "name";
        String direction = "asc";

        Sort sort = Sort.by(Sort.Direction.ASC, sortBy);

        Pageable pageable =
                PageRequest.of(page, size, sort);

        Page<School> emptyPage = Page.empty(pageable);

        SchoolStatus status = SchoolStatus.ACTIVE;

        when(schoolRepository.findAllByStatus(status, pageable))
                .thenReturn(emptyPage);

        // Act
        PageResponse<SchoolResponse> actualResponse =
                schoolService.getAllSchools(
                        page,
                        size,
                        sortBy,
                        direction,
                        status,
                        ""
                );

        // Assert
        assertTrue(actualResponse.content().isEmpty());
        assertEquals(0, actualResponse.totalElements());
        assertEquals(0, actualResponse.totalPages());
        assertTrue(actualResponse.first());
        assertTrue(actualResponse.last());

        verify(schoolRepository).findAllByStatus(status, pageable);
        verifyNoInteractions(schoolMapper);
   }

    @Test
    void getAllSchools_whenPageIsNegative_throwsInvalidRequestException() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> schoolService.getAllSchools(
                        -1,
                        10,
                        "name",
                        "asc",
                        SchoolStatus.ACTIVE,
                        ""
                )
        );

        assertEquals(
                "Page number cannot be negative",
                exception.getMessage()
        );

        verifyNoInteractions(schoolRepository, schoolMapper);
    }

    @Test
    void getAllSchools_whenSizeIsInvalid_throwsInvalidRequestException() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> schoolService.getAllSchools(
                        0,
                        0,
                        "name",
                        "asc",
                        SchoolStatus.ACTIVE,
                        ""
                )
        );

        assertEquals(
                "Page size must be between 1 and 100",
                exception.getMessage()
        );

        verifyNoInteractions(schoolRepository, schoolMapper);
        }

    @Test
    void getAllSchools_whenSortFieldIsInvalid_throwsInvalidRequestException() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> schoolService.getAllSchools(
                        0,
                        10,
                        "unknownField",
                        "asc",
                        SchoolStatus.ACTIVE,
                        ""
                )
        );

        assertEquals(
                "Invalid sort field: unknownField",
                exception.getMessage()
        );

        verifyNoInteractions(schoolRepository, schoolMapper);
  }

    @Test
    void getAllSchools_whenDirectionIsInvalid_throwsInvalidRequestException() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> schoolService.getAllSchools(
                        0,
                        10,
                        "name",
                        "sideways",
                        SchoolStatus.ACTIVE,
                        ""
                )
        );

        assertEquals(
                "Sort direction must be asc or desc",
                exception.getMessage()
        );

        verifyNoInteractions(schoolRepository, schoolMapper);
   }
}