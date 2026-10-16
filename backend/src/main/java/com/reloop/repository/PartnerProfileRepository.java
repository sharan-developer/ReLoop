package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PartnerProfileRepository extends JpaRepository<PartnerProfile, Long> {
    Optional<PartnerProfile> findByUserId(Long userId);
    List<PartnerProfile> findByApprovedTrueOrderByBusinessNameAsc();
    long countByApproved(boolean approved);
    @Query("select p from PartnerProfile p where lower(p.businessName) like lower(concat('%', :q, '%')) or lower(p.user.email) like lower(concat('%', :q, '%'))")
    Page<PartnerProfile> search(@Param("q") String q, Pageable pageable);
}
