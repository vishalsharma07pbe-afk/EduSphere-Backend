package com.edusphere.identity.permission.repository;

import com.edusphere.identity.permission.entity.Permission;
import com.edusphere.identity.permission.enums.PermissionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository
        extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(PermissionCode code);

    boolean existsByCode(PermissionCode code);

    List<Permission> findAllByActiveTrue();
}