package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);
    List<CustomerOrder> findByCreatedAtAfter(Instant since);
    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o where o.status <> :excluded and (o.paymentStatus = :paid or o.paymentMethod = :cod)")
    BigDecimal salesTotal(@Param("excluded") OrderStatus excluded, @Param("paid") PaymentStatus paid, @Param("cod") PaymentMethod cod);
    @Query("select o from CustomerOrder o where lower(o.orderNumber) like lower(concat('%', :q, '%')) or lower(o.user.email) like lower(concat('%', :q, '%'))")
    Page<CustomerOrder> search(@Param("q") String q, Pageable pageable);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CustomerOrder o set o.status = :to where o.id = :id and o.status in :from")
    int transition(@Param("id") Long id, @Param("from") Collection<OrderStatus> from, @Param("to") OrderStatus to);
}
