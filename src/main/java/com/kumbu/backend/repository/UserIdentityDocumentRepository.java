package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.UserIdentityDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserIdentityDocumentRepository extends JpaRepository<UserIdentityDocument, UserIdentityDocument.UserIdentityDocumentId> {

    List<UserIdentityDocument> findByUserIdOrderBySideAsc(UUID userId);

    long countByUserId(UUID userId);
}
