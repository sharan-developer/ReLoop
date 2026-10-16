package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("select p from Product p where p.active = true"
         + " and (:cat is null or p.category.id = :cat)"
         + " and (:cond is null or p.itemCondition = :cond)"
         + " and (:min is null or p.price >= :min)"
         + " and (:max is null or p.price <= :max)"
         + " and (lower(p.name) like lower(concat('%', :q, '%')) or lower(coalesce(p.brand, '')) like lower(concat('%', :q, '%')))")
    Page<Product> search(@Param("q") String q, @Param("cat") Long cat, @Param("cond") ItemCondition cond,
                         @Param("min") BigDecimal min, @Param("max") BigDecimal max, Pageable pageable);
    @Query("select p from Product p where lower(p.name) like lower(concat('%', :q, '%')) or lower(coalesce(p.brand, '')) like lower(concat('%', :q, '%'))")
    Page<Product> adminSearch(@Param("q") String q, Pageable pageable);
    List<Product> findByIdInAndActiveTrue(Collection<Long> ids);
    List<Product> findByCategoryIdAndIdNotAndActiveTrue(Long categoryId, Long id, Pageable pageable);
    long countByActive(boolean active);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.soldCount = p.soldCount + :q where p.id = :id")
    int addSold(@Param("id") Long id, @Param("q") int q);
}
