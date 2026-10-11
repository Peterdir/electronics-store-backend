package com.ecommerce.backend.modules.auth.repository;

import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u
        WHERE COALESCE(:keyword, '') = ''
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        WHERE u.role = :role
            AND u.createdAt >= :startDate AND u.createdAt <= :endDate
    """)
    Long countNewUsersBetween(
            @Param("role") Role role,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}
