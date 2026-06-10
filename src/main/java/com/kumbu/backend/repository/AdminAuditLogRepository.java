package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT a FROM AdminAuditLog a
            WHERE (:action IS NULL OR :action = '' OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%')))
              AND (:q IS NULL OR :q = '' OR LOWER(COALESCE(a.actorEmail, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.entity, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.entityId, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(a.action, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY a.createdAt DESC
            """)
    Page<AdminAuditLog> search(
            @Param("q") String q,
            @Param("action") String action,
            Pageable pageable);
}
