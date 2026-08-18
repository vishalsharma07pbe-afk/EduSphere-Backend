package com.edusphere.identity.user.repository;

import com.edusphere.identity.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByOrganizationIdAndUsername(
            Long organizationId,
            String username
    );

    Optional<User> findByOrganizationIdAndEmail(
            Long organizationId,
            String email
    );

    Optional<User> findByOrganizationIdAndId(
            Long organizationId,
            Long userId
    );

    boolean existsByOrganizationIdAndUsername(
            Long organizationId,
            String username
    );

    boolean existsByOrganizationIdAndEmail(
            Long organizationId,
            String email
    );

    Page<User> findAllByOrganizationId(
            Long organizationId,
            Pageable pageable
    );
}
