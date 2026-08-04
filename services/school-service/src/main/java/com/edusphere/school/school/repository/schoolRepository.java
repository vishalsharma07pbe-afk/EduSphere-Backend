package com.edusphere.school.school.repository;

import com.edusphere.school.school.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface schoolRepository extends JpaRepository<School, Long> {
    boolean existsBySchoolCode(String schoolCode);
}
