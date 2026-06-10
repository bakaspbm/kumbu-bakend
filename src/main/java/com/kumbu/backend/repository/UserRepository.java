package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import java.time.Instant;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE (:status IS NULL OR :status = '' OR :status = 'all'
                OR (:status = 'active' AND u.deletedAt IS NULL
                    AND (u.bannedAt IS NULL OR (u.bannedUntil IS NOT NULL AND u.bannedUntil <= CURRENT_TIMESTAMP)))
                OR (:status = 'deleted' AND u.deletedAt IS NOT NULL)
                OR (:status = 'banned' AND u.deletedAt IS NULL AND u.bannedAt IS NOT NULL
                    AND (u.bannedUntil IS NULL OR u.bannedUntil > CURRENT_TIMESTAMP)))
              AND (:q IS NULL OR :q = '' OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.createdAt DESC
            """)
    Page<User> searchAdmin(
            @Param("q") String q,
            @Param("status") String status,
            Pageable pageable);

    long countByCreatedAtAfter(Instant since);
}
