package com.reloop.repository;

import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ListingReportRepository extends JpaRepository<ListingReport, Long> {
    long countByStatus(ReportStatus status);
    Page<ListingReport> findByStatus(ReportStatus status, Pageable pageable);
    List<ListingReport> findByListingId(Long listingId);
    boolean existsByListingIdAndReporterId(Long listingId, Long reporterId);
}
