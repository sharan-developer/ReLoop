package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndHiddenFalseOrderByCreatedAtDesc(Long productId, Pageable pageable);
    boolean existsByOrderItemId(Long orderItemId);
    long countByProductIdAndHiddenFalse(Long productId);
    @Query("select avg(r.rating) from Review r where r.product.id = :pid and r.hidden = false")
    Double averageRating(@Param("pid") Long productId);
    @Query("select r from Review r where lower(r.product.name) like lower(concat('%', :q, '%')) or lower(r.user.fullName) like lower(concat('%', :q, '%'))")
    Page<Review> search(@Param("q") String q, Pageable pageable);
    @Query("select r.orderItem.id from Review r where r.orderItem.id in :ids")
    List<Long> reviewedItemIds(@Param("ids") Collection<Long> ids);
}
