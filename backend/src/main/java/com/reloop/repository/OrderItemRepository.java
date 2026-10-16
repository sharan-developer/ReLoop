package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderRefId(Long orderId);
    Optional<OrderItem> findByIdAndOrderRefUserId(Long id, Long userId);
    @Query("select oi from OrderItem oi where oi.listing.seller.id = :sid and oi.orderRef.type = :type and oi.orderRef.status in :statuses order by oi.orderRef.createdAt desc")
    List<OrderItem> findSales(@Param("sid") Long sellerId, @Param("type") OrderType type, @Param("statuses") Collection<OrderStatus> statuses);
}
