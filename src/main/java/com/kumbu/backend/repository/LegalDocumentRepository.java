package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, String> {
}
