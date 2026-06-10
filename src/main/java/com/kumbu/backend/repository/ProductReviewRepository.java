package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    List<ProductReview> findByProductIdOrderByCreatedAtDesc(String productId);

    Page<ProductReview> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ProductReview> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);

    boolean existsByProductIdAndUserId(String productId, UUID userId);
}
