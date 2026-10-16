package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RepairRequestRepository extends JpaRepository<RepairRequest, Long> {
    List<RepairRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<RepairRequest> findByPartnerIdOrderByCreatedAtDesc(Long partnerId);
    long countByStatus(RepairStatus status);
    long countByOwnedProductIdAndStatusIn(Long ownedProductId, Collection<RepairStatus> statuses);
    List<RepairRequest> findByCreatedAtAfter(Instant since);
}
