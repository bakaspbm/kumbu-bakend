package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.CatalogProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogProductRepository extends JpaRepository<CatalogProduct, String> {

    Optional<CatalogProduct> findByIdAndDeletedAtIsNull(String id);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE (:q IS NULL OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(p.id, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<CatalogProduct> searchAll(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE p.deletedAt IS NULL
              AND p.featured = true
              AND p.outOfStock = false
              AND (p.jobListingStatus IS NULL OR p.jobListingStatus = 'active')
            """)
    Page<CatalogProduct> findPublicFeatured(Pageable pageable);

    Page<CatalogProduct> findByDeletedAtIsNullAndCategoryId(String categoryId, Pageable pageable);

    List<CatalogProduct> findBySellerIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID sellerId);

    Page<CatalogProduct> findByDeletedAtIsNull(Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE p.deletedAt IS NULL
              AND p.outOfStock = false
              AND (p.jobListingStatus IS NULL OR p.jobListingStatus = 'active')
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:subcategoryId IS NULL OR p.subcategoryId = :subcategoryId)
              AND (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND (:featured IS NULL OR p.featured = :featured)
              AND (:queryPattern IS NULL OR LOWER(p.title) LIKE :queryPattern)
            """)
    Page<CatalogProduct> search(
            @Param("categoryId") String categoryId,
            @Param("subcategoryId") String subcategoryId,
            @Param("sellerId") UUID sellerId,
            @Param("featured") Boolean featured,
            @Param("queryPattern") String queryPattern,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM catalog_products p
            WHERE p.deleted_at IS NULL
              AND p.listing_kind = 'JOB'
              AND p.job_listing_status = 'active'
              AND (:query IS NULL OR :query = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:province IS NULL OR :province = '' OR p.job_meta->>'province' = :province)
              AND (:municipality IS NULL OR :municipality = '' OR p.job_meta->>'municipality' = :municipality)
              AND (:contractType IS NULL OR :contractType = '' OR p.job_meta->>'contractType' = :contractType)
              AND (:sector IS NULL OR :sector = '' OR p.job_meta->>'sector' = :sector)
              AND (:remote IS NULL OR COALESCE((p.job_meta->>'remote')::boolean, false) = :remote)
            ORDER BY p.created_at DESC
            """, nativeQuery = true)
    List<CatalogProduct> findActiveJobsFiltered(
            @Param("query") String query,
            @Param("province") String province,
            @Param("municipality") String municipality,
            @Param("contractType") String contractType,
            @Param("sector") String sector,
            @Param("remote") Boolean remote);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE p.listingKind = com.kumbu.backend.domain.enums.ListingKind.JOB
              AND (:status IS NULL OR p.jobListingStatus = :status)
              AND (:q IS NULL OR :q = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.id, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:includeDeleted = true OR p.deletedAt IS NULL)
            ORDER BY p.createdAt DESC
            """)
    Page<CatalogProduct> adminSearchJobs(
            @Param("status") String status,
            @Param("q") String q,
            @Param("includeDeleted") boolean includeDeleted,
            Pageable pageable);

    @Query("SELECT COUNT(p) FROM CatalogProduct p WHERE p.deletedAt IS NULL")
    long countActiveListings();

    @Query("SELECT COUNT(p) FROM CatalogProduct p WHERE p.deletedAt IS NULL AND p.outOfStock = true")
    long countActiveOutOfStock();

    @Query("""
            SELECT COUNT(DISTINCT p.sellerId) FROM CatalogProduct p
            WHERE p.deletedAt IS NULL AND p.sellerId IS NOT NULL
            """)
    long countDistinctActiveSellers();

    @Query("""
            SELECT p FROM CatalogProduct p
            JOIN p.seller u
            WHERE p.deletedAt IS NULL
              AND p.id <> :excludeId
              AND LOWER(TRIM(COALESCE(u.city, ''))) = LOWER(TRIM(COALESCE(:city, '')))
              AND TRIM(COALESCE(:city, '')) <> ''
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
            ORDER BY p.boostScore DESC, p.viewCount DESC, p.createdAt DESC
            """)
    List<CatalogProduct> findNearbyByCity(
            @Param("excludeId") String excludeId,
            @Param("city") String city,
            @Param("categoryId") String categoryId,
            Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            JOIN p.seller u
            WHERE p.deletedAt IS NULL
              AND p.id <> :excludeId
              AND LOWER(TRIM(COALESCE(u.region, ''))) = LOWER(TRIM(COALESCE(:region, '')))
              AND TRIM(COALESCE(:region, '')) <> ''
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
            ORDER BY p.boostScore DESC, p.viewCount DESC, p.createdAt DESC
            """)
    List<CatalogProduct> findNearbyByRegion(
            @Param("excludeId") String excludeId,
            @Param("region") String region,
            @Param("categoryId") String categoryId,
            Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE p.deletedAt IS NULL
              AND p.id <> :productId
              AND p.categoryId = :categoryId
              AND (:subcategoryId IS NULL OR p.subcategoryId = :subcategoryId
                   OR (:keyword IS NOT NULL AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))))
            ORDER BY
              CASE WHEN :subcategoryId IS NOT NULL AND p.subcategoryId = :subcategoryId THEN 0 ELSE 1 END,
              p.viewCount DESC,
              p.boostScore DESC
            """)
    List<CatalogProduct> findSimilar(
            @Param("productId") String productId,
            @Param("categoryId") String categoryId,
            @Param("subcategoryId") String subcategoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE p.deletedAt IS NULL
              AND p.sellerId = :sellerId
              AND p.id <> :excludeId
            ORDER BY p.viewCount DESC, p.createdAt DESC
            """)
    List<CatalogProduct> findOtherBySeller(
            @Param("sellerId") UUID sellerId,
            @Param("excludeId") String excludeId,
            Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE p.deletedAt IS NULL
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
            ORDER BY p.viewCount DESC, p.boostScore DESC, p.createdAt DESC
            """)
    List<CatalogProduct> findTrending(
            @Param("categoryId") String categoryId,
            Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            JOIN p.seller u
            WHERE p.deletedAt IS NULL
              AND p.categoryId IN :categoryIds
              AND p.id NOT IN :excludeIds
              AND (
                TRIM(COALESCE(:city, '')) = ''
                OR LOWER(TRIM(COALESCE(u.city, ''))) = LOWER(TRIM(:city))
                OR LOWER(TRIM(COALESCE(u.region, ''))) = LOWER(TRIM(COALESCE(:region, '')))
              )
            ORDER BY
              CASE WHEN LOWER(TRIM(COALESCE(u.city, ''))) = LOWER(TRIM(COALESCE(:city, ''))) THEN 0 ELSE 1 END,
              p.boostScore DESC,
              p.viewCount DESC
            """)
    List<CatalogProduct> findForYou(
            @Param("categoryIds") List<String> categoryIds,
            @Param("excludeIds") List<String> excludeIds,
            @Param("city") String city,
            @Param("region") String region,
            Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            JOIN p.seller u
            WHERE p.deletedAt IS NULL
              AND p.createdAt >= :since
              AND (
                TRIM(COALESCE(:city, '')) = ''
                OR LOWER(TRIM(COALESCE(u.city, ''))) = LOWER(TRIM(:city))
              )
            ORDER BY p.createdAt DESC
            """)
    List<CatalogProduct> findNewNearby(
            @Param("city") String city,
            @Param("since") java.time.Instant since,
            Pageable pageable);

    List<CatalogProduct> findByIdInAndDeletedAtIsNull(List<String> ids);

    Page<CatalogProduct> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<CatalogProduct> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    @Query("""
            SELECT p FROM CatalogProduct p
            WHERE (:onlyDeleted = false OR p.deletedAt IS NOT NULL)
              AND (:onlyDeleted = true OR :includeDeleted = true OR p.deletedAt IS NULL)
              AND (:sellerId IS NULL OR p.sellerId = :sellerId)
              AND (:categoryId IS NULL OR :categoryId = '' OR p.categoryId = :categoryId)
              AND (:outOfStock IS NULL OR p.outOfStock = :outOfStock)
              AND (:q IS NULL OR :q = '' OR LOWER(COALESCE(p.title, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(p.id, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.createdAt DESC
            """)
    Page<CatalogProduct> adminListProducts(
            @Param("q") String q,
            @Param("sellerId") UUID sellerId,
            @Param("categoryId") String categoryId,
            @Param("outOfStock") Boolean outOfStock,
            @Param("includeDeleted") boolean includeDeleted,
            @Param("onlyDeleted") boolean onlyDeleted,
            Pageable pageable);
}
