package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.CatalogProduct;
import com.kumbu.backend.domain.entity.JobApplication;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.ListingKind;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.JobApplicationRepository;
import com.kumbu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminJobsService {

    private final CatalogProductRepository productRepository;
    private final JobApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final AdminManagementService adminManagementService;

    @Transactional(readOnly = true)
    public Map<String, Object> listJobListings(String status, String q, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<CatalogProduct> result = productRepository.adminSearchJobs(
                blankToNull(status), blankToNull(q), false, pageable);
        List<Map<String, Object>> items = result.getContent().stream()
                .map(this::toJobListingMap)
                .toList();
        return pageResult(items, result);
    }

    @Transactional
    public Map<String, Object> updateJobListingStatus(String jobId, String status) {
        CatalogProduct job = productRepository.findById(jobId)
                .filter(p -> p.getListingKind() == ListingKind.JOB)
                .orElseThrow(() -> ApiException.notFound("Vaga não encontrada"));
        if (!List.of("active", "filled_hidden").contains(status)) {
            throw ApiException.badRequest("Estado inválido");
        }
        job.setJobListingStatus(status);
        productRepository.save(job);
        return toJobListingMap(job);
    }

    @Transactional
    public void softDeleteJobListing(String jobId) {
        adminManagementService.softDeleteProduct(jobId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listApplications(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<JobApplication> result = blankToNull(status) == null
                ? applicationRepository.findAllByOrderByCreatedAtDesc(pageable)
                : applicationRepository.findByStatusOrderByCreatedAtDesc(status.trim().toLowerCase(), pageable);
        List<Map<String, Object>> items = result.getContent().stream()
                .map(this::toApplicationListMap)
                .toList();
        return pageResult(items, result);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getApplication(UUID id) {
        JobApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Candidatura não encontrada"));
        return toApplicationDetailMap(app);
    }

    @Transactional(readOnly = true)
    public long countPendingApplications() {
        return applicationRepository.countByStatus("pending");
    }

    private Map<String, Object> toJobListingMap(CatalogProduct job) {
        User seller = userRepository.findById(job.getSellerId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", job.getId());
        map.put("title", job.getTitle());
        map.put("price_label", job.getPriceLabel());
        map.put("category_id", job.getCategoryId());
        map.put("job_listing_status", job.getJobListingStatus());
        map.put("job_meta", job.getJobMeta());
        map.put("seller_id", job.getSellerId());
        map.put("seller_name", seller != null ? seller.getDisplayName() : null);
        map.put("seller_email", seller != null ? seller.getEmail() : null);
        map.put("created_at", job.getCreatedAt());
        map.put("deleted_at", job.getDeletedAt());
        return map;
    }

    private Map<String, Object> toApplicationListMap(JobApplication app) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", app.getId());
        map.put("job_id", app.getJobId());
        map.put("job_title", resolveJobTitle(app.getJobId()));
        map.put("applicant_id", app.getApplicantId());
        map.put("employer_id", app.getEmployerId());
        map.put("status", app.getStatus());
        map.put("created_at", app.getCreatedAt());
        map.put("applicant_name", userLabel(app.getApplicantId()));
        map.put("employer_name", userLabel(app.getEmployerId()));
        map.put("cv_title", cvTitle(app));
        return map;
    }

    private Map<String, Object> toApplicationDetailMap(JobApplication app) {
        Map<String, Object> map = toApplicationListMap(app);
        map.put("cover_message", app.getCoverMessage());
        map.put("conversation_id", app.getConversationId());
        map.put("cv_id", app.getCvId());
        map.put("cv_snapshot", app.getCvSnapshot());
        map.put("cv_viewed_at", app.getCvViewedAt());
        map.put("updated_at", app.getUpdatedAt());
        map.put("applicant_email", userEmail(app.getApplicantId()));
        map.put("employer_email", userEmail(app.getEmployerId()));
        return map;
    }

    private String resolveJobTitle(String jobId) {
        return productRepository.findById(jobId).map(CatalogProduct::getTitle).orElse(jobId);
    }

    private String userLabel(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getEmail())
                .orElse(userId.toString().substring(0, 8));
    }

    private String userEmail(UUID userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }

    private static String cvTitle(JobApplication app) {
        if (app.getCvSnapshot() == null) return null;
        Object title = app.getCvSnapshot().get("title");
        return title != null ? String.valueOf(title) : null;
    }

    private static Map<String, Object> pageResult(List<Map<String, Object>> items, Page<?> page) {
        return Map.of(
                "items", items,
                "page", page.getNumber(),
                "size", page.getSize(),
                "total", page.getTotalElements(),
                "total_pages", page.getTotalPages()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
