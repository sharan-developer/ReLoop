package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OwnedProductRepository extends JpaRepository<OwnedProduct, Long> {
    List<OwnedProduct> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<OwnedProduct> findByIdAndOwnerId(Long id, Long ownerId);
    Optional<OwnedProduct> findByPublicToken(String token);
}
