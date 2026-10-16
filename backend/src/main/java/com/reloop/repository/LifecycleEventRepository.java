package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LifecycleEventRepository extends JpaRepository<LifecycleEvent, Long> {
    List<LifecycleEvent> findByOwnedProductIdOrderByCreatedAtAscIdAsc(Long ownedProductId);
    @Query("select e from LifecycleEvent e where lower(e.ownedProduct.name) like lower(concat('%', :q, '%')) or lower(e.details) like lower(concat('%', :q, '%'))")
    Page<LifecycleEvent> search(@Param("q") String q, Pageable pageable);
}
