package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OwnershipTransferRepository extends JpaRepository<OwnershipTransfer, Long> {
    List<OwnershipTransfer> findByCreatedAtAfter(Instant since);
    boolean existsByOrderId(Long orderId);
}
