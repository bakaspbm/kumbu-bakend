package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.CatalogProduct;
import com.kumbu.backend.domain.entity.Conversation;
import com.kumbu.backend.domain.entity.PropertyRentalRequest;
import com.kumbu.backend.domain.entity.UserNotification;
import com.kumbu.backend.domain.enums.DealStatus;
import com.kumbu.backend.domain.enums.ListingKind;
import com.kumbu.backend.dto.rental.CreateRentalRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.ConversationRepository;
import com.kumbu.backend.repository.PropertyRentalRequestRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final PropertyRentalRequestRepository rentalRepository;
    private final CatalogProductRepository productRepository;
    private final ConversationRepository conversationRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<RentalDto> listMine(String role) {
        UUID userId = securityUtils.currentUserId();
        List<PropertyRentalRequest> list = "owner".equalsIgnoreCase(role)
                ? rentalRepository.findByOwnerIdOrderByCreatedAtDesc(userId)
                : rentalRepository.findByRenterIdOrderByCreatedAtDesc(userId);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public RentalDto create(CreateRentalRequest request) {
        if ("daily".equalsIgnoreCase(request.getRentalMode())) {
            if (request.getCheckIn() == null || request.getCheckOut() == null) {
                throw ApiException.badRequest("Check-in e check-out são obrigatórios para aluguer diário");
            }
            if (!request.getCheckOut().isAfter(request.getCheckIn())) {
                throw ApiException.badRequest("Check-out deve ser posterior ao check-in");
            }
        }
        UUID renterId = securityUtils.currentUserId();
        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(request.getProductId())
                .filter(p -> p.getListingKind() == ListingKind.PROPERTY)
                .orElseThrow(() -> ApiException.notFound("Imóvel não encontrado"));

        if ("daily".equals(request.getRentalMode()) && request.getCheckIn() != null && request.getCheckOut() != null) {
            if (rentalRepository.existsOverlap(request.getProductId(), request.getCheckIn(), request.getCheckOut())) {
                throw ApiException.conflict("Datas indisponíveis");
            }
        }

        Integer nights = null;
        if (request.getCheckIn() != null && request.getCheckOut() != null) {
            nights = (int) ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());
        }

        Conversation conv = conversationRepository.save(Conversation.builder()
                .productId(request.getProductId())
                .buyerId(renterId)
                .sellerId(product.getSellerId())
                .dealStatus(DealStatus.OPEN)
                .build());

        PropertyRentalRequest rental = PropertyRentalRequest.builder()
                .productId(request.getProductId())
                .renterId(renterId)
                .ownerId(product.getSellerId())
                .rentalMode(request.getRentalMode())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .nights(nights)
                .guestMessage(request.getGuestMessage())
                .priceSnapshot(request.getPriceSnapshot())
                .conversationId(conv.getId())
                .build();

        PropertyRentalRequest saved = rentalRepository.save(rental);
        notifyOwnerNewRental(product, saved);
        return toDto(saved);
    }

    @Transactional
    public RentalDto respond(UUID requestId, String action) {
        UUID ownerId = securityUtils.currentUserId();
        PropertyRentalRequest rental = rentalRepository.findById(requestId)
                .filter(r -> ownerId.equals(r.getOwnerId()))
                .orElseThrow(() -> ApiException.notFound("Pedido não encontrado"));

        rental.setStatus(switch (action.toLowerCase()) {
            case "confirm", "confirmed" -> "confirmed";
            case "reject", "rejected" -> "rejected";
            default -> throw ApiException.badRequest("Acção inválida");
        });
        PropertyRentalRequest saved = rentalRepository.save(rental);
        notifyRenterResponse(saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DateRangeDto> occupiedRanges(String productId) {
        return rentalRepository.findOccupiedRanges(productId).stream()
                .map(r -> new DateRangeDto(r.getCheckIn(), r.getCheckOut()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isDailyRangeAvailable(String productId, LocalDate checkIn, LocalDate checkOut) {
        return !rentalRepository.existsOverlap(productId, checkIn, checkOut);
    }

    private void notifyOwnerNewRental(CatalogProduct product, PropertyRentalRequest rental) {
        String listingTitle = product.getTitle() != null ? product.getTitle().trim() : "Imóvel";
        String body = buildRentalRequestBody(rental, listingTitle);
        notificationService.saveAndPush(UserNotification.builder()
                .userId(rental.getOwnerId())
                .title("Novo pedido de aluguer")
                .body(body)
                .iconKey("home_outlined")
                .actionUrl("/conta/reservas?tab=owner")
                .build());
    }

    private void notifyRenterResponse(PropertyRentalRequest rental) {
        boolean confirmed = "confirmed".equalsIgnoreCase(rental.getStatus());
        String actionUrl = confirmed && rental.getConversationId() != null
                ? "/mensagens/" + rental.getConversationId()
                : "/conta/reservas";
        notificationService.saveAndPush(UserNotification.builder()
                .userId(rental.getRenterId())
                .title(confirmed ? "Reserva confirmada" : "Pedido de aluguer recusado")
                .body(confirmed
                        ? "O proprietário confirmou o seu pedido. Abra o chat para combinar detalhes."
                        : "O proprietário não confirmou o pedido de aluguer desta vez.")
                .iconKey("home_outlined")
                .actionUrl(actionUrl)
                .build());
    }

    private static String buildRentalRequestBody(PropertyRentalRequest rental, String listingTitle) {
        if ("daily".equalsIgnoreCase(rental.getRentalMode())
                && rental.getCheckIn() != null
                && rental.getCheckOut() != null) {
            return "Pedido para " + rental.getCheckIn() + " → " + rental.getCheckOut()
                    + " — " + listingTitle;
        }
        return "Pedido de aluguer longo prazo — " + listingTitle;
    }

    private RentalDto toDto(PropertyRentalRequest r) {
        return RentalDto.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .renterId(r.getRenterId())
                .ownerId(r.getOwnerId())
                .rentalMode(r.getRentalMode())
                .checkIn(r.getCheckIn())
                .checkOut(r.getCheckOut())
                .nights(r.getNights())
                .guestMessage(r.getGuestMessage())
                .status(r.getStatus())
                .conversationId(r.getConversationId())
                .priceSnapshot(r.getPriceSnapshot())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Data @Builder public static class RentalDto {
        private UUID id; private String productId; private UUID renterId; private UUID ownerId;
        private String rentalMode; private LocalDate checkIn; private LocalDate checkOut;
        private Integer nights; private String guestMessage; private String status;
        private UUID conversationId; private String priceSnapshot; private java.time.Instant createdAt;
    }

    public record DateRangeDto(LocalDate checkIn, LocalDate checkOut) {}
}
