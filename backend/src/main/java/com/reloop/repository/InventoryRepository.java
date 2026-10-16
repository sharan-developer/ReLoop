package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);
    List<Inventory> findByProductIdIn(Collection<Long> productIds);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Inventory i set i.quantity = i.quantity - :q where i.product.id = :pid and i.quantity >= :q")
    int decrement(@Param("pid") Long productId, @Param("q") int q);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Inventory i set i.quantity = i.quantity + :q where i.product.id = :pid")
    int restock(@Param("pid") Long productId, @Param("q") int q);
}
