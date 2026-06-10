package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.ProductViewEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductViewEventRepository extends JpaRepository<ProductViewEvent, UUID> {

    List<ProductViewEvent> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
            SELECT e.categoryId, COUNT(e) AS cnt FROM ProductViewEvent e
            WHERE e.userId = :userId AND e.categoryId IS NOT NULL
            GROUP BY e.categoryId
            ORDER BY cnt DESC
            """)
    List<Object[]> countCategoriesByUser(@Param("userId") UUID userId, Pageable pageable);
}
