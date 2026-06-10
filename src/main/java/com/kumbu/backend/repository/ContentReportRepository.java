package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {
    Page<ContentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(String status);

    List<ContentReport> findByReporterIdOrderByCreatedAtDesc(UUID reporterId);

    Page<ContentReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<ContentReport> findByReportedUserIdOrderByCreatedAtDesc(UUID reportedUserId, Pageable pageable);

    Page<ContentReport> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);
}
