package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Optional<JobApplication> findByJobIdAndApplicantId(String jobId, UUID applicantId);

    List<JobApplication> findByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

    @Query("""
            SELECT a FROM JobApplication a
            WHERE a.employerId = :employerId
              AND (:status IS NULL OR :status = '' OR a.status = :status)
            ORDER BY a.createdAt DESC
            """)
    List<JobApplication> findByEmployer(
            @Param("employerId") UUID employerId,
            @Param("status") String status);

    Page<JobApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<JobApplication> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);
}
