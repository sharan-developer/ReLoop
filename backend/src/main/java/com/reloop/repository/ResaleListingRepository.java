package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ResaleListingRepository extends JpaRepository<ResaleListing, Long> {
    @Query("select l from ResaleListing l where l.status = :status"
         + " and (:cond is null or l.itemCondition = :cond)"
         + " and (:min is null or l.price >= :min)"
         + " and (:max is null or l.price <= :max)"
         + " and (lower(l.title) like lower(concat('%', :q, '%')))")
    Page<ResaleListing> searchPublic(@Param("status") ListingStatus status, @Param("q") String q, @Param("cond") ItemCondition cond,
                                     @Param("min") BigDecimal min, @Param("max") BigDecimal max, Pageable pageable);
    List<ResaleListing> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    boolean existsByOwnedProductIdAndStatusIn(Long ownedProductId, Collection<ListingStatus> statuses);
    long countByStatus(ListingStatus status);
    List<ResaleListing> findByOwnedProductIdAndStatusIn(Long ownedProductId, Collection<ListingStatus> statuses);
    @Query("select l from ResaleListing l where lower(l.title) like lower(concat('%', :q, '%'))")
    Page<ResaleListing> adminSearch(@Param("q") String q, Pageable pageable);
    /** Atomic reservation: succeeds only if the listing is still PUBLISHED and the buyer is not the seller. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ResaleListing l set l.status = :to where l.id = :id and l.status = :from and l.seller.id <> :buyerId")
    int reserve(@Param("id") Long id, @Param("from") ListingStatus from, @Param("to") ListingStatus to, @Param("buyerId") Long buyerId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ResaleListing l set l.status = :to where l.id = :id and l.status = :from")
    int transition(@Param("id") Long id, @Param("from") ListingStatus from, @Param("to") ListingStatus to);
}
