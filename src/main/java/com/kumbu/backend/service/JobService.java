package com.kumbu.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.*;
import com.kumbu.backend.domain.enums.DealStatus;
import com.kumbu.backend.domain.enums.ListingKind;
import com.kumbu.backend.dto.catalog.ListingResponse;
import com.kumbu.backend.dto.job.CvCreateRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.*;
import com.kumbu.backend.security.SecurityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final UserCvRepository cvRepository;
    private final JobApplicationRepository applicationRepository;
    private final CatalogProductRepository productRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final CatalogService catalogService;

    @Transactional(readOnly = true)
    public List<CvDto> listMyCvs() {
        return cvRepository.findByUserIdOrderByUpdatedAtDesc(securityUtils.currentUserId())
                .stream().map(this::toCvDto).toList();
    }

    @Transactional
    public CvDto createCv(CvCreateRequest request) {
        UserCv cv = UserCv.builder()
                .userId(securityUtils.currentUserId())
                .title(request.getTitle())
                .fullName(request.getFullName())
                .profession(request.getProfession())
                .email(request.getEmail())
                .phone(request.getPhone())
                .city(request.getCity())
                .province(request.getProvince())
                .summary(request.getSummary())
                .skills(request.getSkills())
                .languages(request.getLanguages())
                .build();
        return toCvDto(cvRepository.save(cv));
    }

    @Transactional
    public void deleteCv(UUID cvId) {
        UserCv cv = cvRepository.findById(cvId)
                .filter(c -> securityUtils.currentUserId().equals(c.getUserId()))
                .orElseThrow(() -> ApiException.notFound("CV não encontrado"));
        cvRepository.delete(cv);
    }

    @Transactional
    public CvDto updateCv(UUID cvId, CvCreateRequest request) {
        UserCv cv = cvRepository.findById(cvId)
                .filter(c -> securityUtils.currentUserId().equals(c.getUserId()))
                .orElseThrow(() -> ApiException.notFound("CV não encontrado"));
        cv.setTitle(request.getTitle());
        cv.setFullName(request.getFullName());
        cv.setProfession(request.getProfession());
        cv.setEmail(request.getEmail());
        cv.setPhone(request.getPhone());
        cv.setCity(request.getCity());
        cv.setProvince(request.getProvince());
        cv.setSummary(request.getSummary());
        cv.setSkills(request.getSkills());
        cv.setLanguages(request.getLanguages());
        return toCvDto(cvRepository.save(cv));
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.JOBS,
            key = "T(java.util.Objects).hash(#query, #province, #municipality, #contractType, #sector, #remote)")
    public List<ListingResponse> listActiveJobs(
            String query,
            String province,
            String municipality,
            String contractType,
            String sector,
            Boolean remote) {
        String q = query == null || query.isBlank() ? null : query.trim();
        return productRepository.findActiveJobsFiltered(
                        q,
                        blank(province),
                        blank(municipality),
                        blank(contractType),
                        blank(sector),
                        remote)
                .stream()
                .map(catalogService::toBrowseListing)
                .toList();
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public ApplicationDto apply(String jobId, UUID cvId, String coverMessage) {
        UUID applicantId = securityUtils.currentUserId();
        CatalogProduct job = productRepository.findByIdAndDeletedAtIsNull(jobId)
                .filter(p -> p.getListingKind() == ListingKind.JOB)
                .orElseThrow(() -> ApiException.notFound("Emprego não encontrado"));

        if (job.getJobListingStatus() != null && !"active".equals(job.getJobListingStatus())) {
            throw ApiException.badRequest("Esta vaga já foi preenchida ou está fechada");
        }
        if (job.isOutOfStock()) {
            throw ApiException.badRequest("Esta vaga já não está disponível");
        }

        if (applicationRepository.findByJobIdAndApplicantId(jobId, applicantId).isPresent()) {
            throw ApiException.conflict("Já se candidatou a este emprego");
        }

        UserCv cv = cvRepository.findById(cvId)
                .filter(c -> applicantId.equals(c.getUserId()))
                .orElseThrow(() -> ApiException.notFound("CV não encontrado"));

        JobApplication app = JobApplication.builder()
                .jobId(jobId)
                .applicantId(applicantId)
                .employerId(job.getSellerId())
                .cvId(cvId)
                .coverMessage(coverMessage)
                .cvSnapshot(objectMapper.convertValue(cv, new TypeReference<Map<String, Object>>() {}))
                .build();

        Conversation conv = conversationRepository.save(Conversation.builder()
                .productId(jobId)
                .buyerId(applicantId)
                .sellerId(job.getSellerId())
                .dealStatus(DealStatus.OPEN)
                .build());
        app.setConversationId(conv.getId());
        applicationRepository.save(app);

        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conv.getId())
                .senderId(applicantId)
                .body("Candidatura enviada" + (coverMessage != null ? ": " + coverMessage : ""))
                .messageKind("job_application")
                .build());

        notifyEmployerNewApplication(job);
        return toAppDto(app);
    }

    private void notifyEmployerNewApplication(CatalogProduct job) {
        String title = job.getTitle() != null ? job.getTitle().trim() : "Emprego";
        notificationService.saveAndPush(UserNotification.builder()
                .userId(job.getSellerId())
                .title("Nova candidatura")
                .body("Candidatura recebida — " + title)
                .iconKey("work_outlined")
                .actionUrl("/conta/vagas-candidaturas")
                .build());
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> listMyApplications() {
        return applicationRepository.findByApplicantIdOrderByCreatedAtDesc(securityUtils.currentUserId())
                .stream().map(this::toAppDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> listEmployerApplications(String status, String q, String province) {
        String normalizedQ = q == null || q.isBlank() ? null : q.trim().toLowerCase();
        return applicationRepository.findByEmployer(securityUtils.currentUserId(), blank(status)).stream()
                .filter(app -> normalizedQ == null || matchesApplicationQuery(app, normalizedQ))
                .filter(app -> province == null || province.isBlank() || matchesApplicationProvince(app, province))
                .map(this::toAppDto)
                .toList();
    }

    private boolean matchesApplicationQuery(JobApplication app, String q) {
        String applicantName = app.getCvSnapshot() != null && app.getCvSnapshot().get("fullName") != null
                ? String.valueOf(app.getCvSnapshot().get("fullName")).toLowerCase()
                : "";
        String jobTitle = resolveJobTitle(app.getJobId()).toLowerCase();
        return applicantName.contains(q) || jobTitle.contains(q);
    }

    private boolean matchesApplicationProvince(JobApplication app, String province) {
        if (app.getCvSnapshot() == null || app.getCvSnapshot().get("province") == null) {
            return false;
        }
        return province.equals(String.valueOf(app.getCvSnapshot().get("province")));
    }

    @Transactional
    public ApplicationDto respond(UUID applicationId, String action) {
        UUID employerId = securityUtils.currentUserId();
        JobApplication app = applicationRepository.findById(applicationId)
                .filter(a -> employerId.equals(a.getEmployerId()))
                .orElseThrow(() -> ApiException.notFound("Candidatura não encontrada"));

        String newStatus = switch (action.toLowerCase()) {
            case "accept", "accepted" -> "accepted";
            case "reject", "rejected" -> "rejected";
            default -> throw ApiException.badRequest("Acção inválida");
        };

        if (newStatus.equals(app.getStatus())) {
            return toAppDto(app);
        }

        app.setStatus(newStatus);
        JobApplication saved = applicationRepository.save(app);
        notifyApplicantResponse(saved, employerId);
        return toAppDto(saved);
    }

    @Transactional
    public void markJobFilled(String jobId) {
        CatalogProduct job = productRepository.findById(jobId)
                .filter(p -> securityUtils.currentUserId().equals(p.getSellerId()))
                .orElseThrow(() -> ApiException.notFound("Emprego não encontrado"));
        job.setJobListingStatus("filled_hidden");
        productRepository.save(job);
    }

    @Transactional
    public CvViewResponse recordCvView(UUID applicationId) {
        UUID employerId = securityUtils.currentUserId();
        JobApplication app = applicationRepository.findById(applicationId)
                .filter(a -> employerId.equals(a.getEmployerId()))
                .orElseThrow(() -> ApiException.notFound("Candidatura não encontrada"));

        if (app.getCvViewedAt() != null) {
            return new CvViewResponse(false, false);
        }

        app.setCvViewedAt(Instant.now());
        applicationRepository.save(app);

        notifyApplicantCvViewed(app, employerId);

        return new CvViewResponse(true, true);
    }

    private void notifyApplicantCvViewed(JobApplication app, UUID employerId) {
        String jobTitle = resolveJobTitle(app.getJobId());
        String employerName = resolveEmployerName(employerId);
        notificationService.saveAndPush(UserNotification.builder()
                .userId(app.getApplicantId())
                .title("O seu CV foi visualizado")
                .body(employerName + " visualizou o seu CV para a vaga «" + jobTitle + "».")
                .iconKey("visibility_outlined")
                .actionUrl("/conta/candidaturas")
                .build());
    }

    private void notifyApplicantResponse(JobApplication app, UUID employerId) {
        boolean accepted = "accepted".equalsIgnoreCase(app.getStatus());
        String jobTitle = resolveJobTitle(app.getJobId());
        String employerName = resolveEmployerName(employerId);
        String actionUrl = accepted && app.getConversationId() != null
                ? "/mensagens/" + app.getConversationId()
                : "/conta/candidaturas";
        notificationService.saveAndPush(UserNotification.builder()
                .userId(app.getApplicantId())
                .title(accepted ? "Candidatura aceite" : "Candidatura recusada")
                .body(accepted
                        ? employerName + " aceitou a sua candidatura para «" + jobTitle + "». Abra o chat para combinar os próximos passos."
                        : employerName + " não avançou com a sua candidatura para «" + jobTitle + "» desta vez.")
                .iconKey("work_outlined")
                .actionUrl(actionUrl)
                .build());
    }

    private String resolveJobTitle(String jobId) {
        return productRepository.findById(jobId)
                .map(CatalogProduct::getTitle)
                .filter(title -> title != null && !title.isBlank())
                .map(String::trim)
                .orElse("Vaga");
    }

    private String resolveEmployerName(UUID employerId) {
        return userRepository.findById(employerId)
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Um empregador");
    }

    private CvDto toCvDto(UserCv cv) {
        return CvDto.builder()
                .id(cv.getId())
                .userId(cv.getUserId())
                .title(cv.getTitle())
                .fullName(cv.getFullName())
                .profession(cv.getProfession())
                .email(cv.getEmail())
                .phone(cv.getPhone())
                .city(cv.getCity())
                .province(cv.getProvince())
                .summary(cv.getSummary())
                .skills(cv.getSkills())
                .languages(cv.getLanguages())
                .build();
    }

    private ApplicationDto toAppDto(JobApplication a) {
        String jobTitle = productRepository.findById(a.getJobId())
                .map(CatalogProduct::getTitle)
                .orElse(null);
        String applicantName = a.getCvSnapshot() != null && a.getCvSnapshot().get("fullName") != null
                ? String.valueOf(a.getCvSnapshot().get("fullName"))
                : null;
        return ApplicationDto.builder()
                .id(a.getId())
                .jobId(a.getJobId())
                .applicantId(a.getApplicantId())
                .employerId(a.getEmployerId())
                .cvId(a.getCvId())
                .status(a.getStatus())
                .coverMessage(a.getCoverMessage())
                .conversationId(a.getConversationId())
                .cvSnapshot(a.getCvSnapshot())
                .jobTitle(jobTitle)
                .applicantName(applicantName)
                .cvViewedAt(a.getCvViewedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }

    @Data @Builder public static class CvDto {
        private UUID id; private UUID userId; private String title; private String fullName;
        private String profession; private String email; private String phone; private String city;
        private String province; private String summary; private List<String> skills; private List<String> languages;
    }

    @Data @Builder public static class JobSummaryDto {
        private String id; private String title; private String priceLabel; private String location; private Object jobMeta;
    }

    @Data @Builder public static class ApplicationDto {
        private UUID id; private String jobId; private UUID applicantId; private UUID employerId;
        private UUID cvId; private String status; private String coverMessage; private UUID conversationId;
        private Map<String, Object> cvSnapshot; private String jobTitle; private String applicantName;
        private Instant cvViewedAt; private Instant createdAt;
    }

    public record CvViewResponse(boolean notified, boolean firstView) {}
}
