package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every mutating admin action is validated and written to audit_logs. Metrics come from database queries only. */
@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository users;
    private final ProductRepository products;
    private final ResaleListingRepository listings;
    private final CustomerOrderRepository orders;
    private final RepairRequestRepository repairs;
    private final PartnerProfileRepository partners;
    private final ListingReportRepository reports;
    private final ReviewRepository reviews;
    private final LifecycleEventRepository events;
    private final OwnershipTransferRepository transfers;
    private final OwnedProductRepository owned;
    private final AuditLogRepository auditRepo;
    private final AuditService audit;

    private static Pageable pg(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sort);
    }

    // ---------------- stats ----------------
    @Transactional(readOnly = true)
    public AdminStats stats() {
        Instant since = YearMonth.now(ZoneOffset.UTC).minusMonths(5).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, long[]> sales = months();
        Map<String, BigDecimal> salesAmt = new LinkedHashMap<>();
        sales.keySet().forEach(k -> salesAmt.put(k, BigDecimal.ZERO));
        for (CustomerOrder o : orders.findByCreatedAtAfter(since)) {
            if (o.getStatus() == OrderStatus.CANCELLED || o.getType() != OrderType.STORE) continue;
            String k = key(o.getCreatedAt());
            if (sales.containsKey(k)) { sales.get(k)[0]++; salesAmt.merge(k, o.getTotal(), BigDecimal::add); }
        }
        Map<String, long[]> rep = months();
        for (RepairRequest r : repairs.findByCreatedAtAfter(since)) {
            String k = key(r.getCreatedAt());
            if (rep.containsKey(k)) rep.get(k)[0]++;
        }
        Map<String, long[]> resale = months();
        for (OwnershipTransfer t : transfers.findByCreatedAtAfter(since)) {
            String k = key(t.getCreatedAt());
            if (resale.containsKey(k)) resale.get(k)[0]++;
        }
        List<TrendPoint> salesTrend = sales.entrySet().stream().map(e -> new TrendPoint(e.getKey(), e.getValue()[0], salesAmt.get(e.getKey()))).toList();
        List<TrendPoint> repairTrend = rep.entrySet().stream().map(e -> new TrendPoint(e.getKey(), e.getValue()[0], BigDecimal.ZERO)).toList();
        List<TrendPoint> resaleTrend = resale.entrySet().stream().map(e -> new TrendPoint(e.getKey(), e.getValue()[0], BigDecimal.ZERO)).toList();
        List<ActivityDto> recent = auditRepo.findTop10ByOrderByCreatedAtDesc().stream()
                .map(a -> new ActivityDto(a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt())).toList();
        return new AdminStats(users.count(), products.countByActive(true), listings.countByStatus(ListingStatus.PUBLISHED), orders.count(),
                repairs.count(), repairs.countByStatus(RepairStatus.COMPLETED), partners.countByApproved(false),
                reports.countByStatus(ReportStatus.OPEN), orders.salesTotal(OrderStatus.CANCELLED, PaymentStatus.PAID, PaymentMethod.COD),
                recent, salesTrend, repairTrend, resaleTrend);
    }

    private static Map<String, long[]> months() {
        Map<String, long[]> m = new LinkedHashMap<>();
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        for (int i = 5; i >= 0; i--) m.put(now.minusMonths(i).toString(), new long[]{0});
        return m;
    }

    private static String key(Instant t) { return YearMonth.from(t.atZone(ZoneOffset.UTC)).toString(); }

    // ---------------- users ----------------
    @Transactional(readOnly = true)
    public PageDto<AdminUserDto> users(String q, int page, int size) {
        Page<User> p = users.search(CatalogService.norm(q), pg(page, size, Sort.by("id").descending()));
        return new PageDto<>(p.getContent().stream().map(u -> new AdminUserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled(), u.getCreatedAt())).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional
    public AdminUserDto setUserEnabled(User admin, Long id, boolean enabled) {
        if (admin.getId().equals(id)) throw ApiException.badRequest("You cannot change your own account status");
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setEnabled(enabled);
        users.save(u);
        audit.log(admin.getId(), enabled ? "USER_ENABLED" : "USER_DISABLED", "User", id, u.getEmail());
        return new AdminUserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }

    // ---------------- partners ----------------
    @Transactional(readOnly = true)
    public PageDto<AdminPartnerDto> partners(String q, int page, int size) {
        Page<PartnerProfile> p = partners.search(CatalogService.norm(q), pg(page, size, Sort.by("id").descending()));
        return new PageDto<>(p.getContent().stream().map(x -> new AdminPartnerDto(x.getId(), x.getUser().getId(), x.getUser().getEmail(),
                x.getBusinessName(), x.getCity(), x.isApproved())).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    /** Approval is the ONLY way a user becomes REPAIR_PARTNER. */
    @Transactional
    public AdminPartnerDto setPartnerApproved(User admin, Long id, boolean approved) {
        PartnerProfile pp = partners.findById(id).orElseThrow(() -> ApiException.notFound("Partner"));
        User u = pp.getUser();
        if (u.getRole() == Role.ADMIN) throw ApiException.badRequest("Administrators cannot be repair partners");
        pp.setApproved(approved);
        partners.save(pp);
        u.setRole(approved ? Role.REPAIR_PARTNER : Role.CUSTOMER);
        users.save(u);
        audit.log(admin.getId(), approved ? "PARTNER_APPROVED" : "PARTNER_REVOKED", "PartnerProfile", id, pp.getBusinessName());
        return new AdminPartnerDto(pp.getId(), u.getId(), u.getEmail(), pp.getBusinessName(), pp.getCity(), pp.isApproved());
    }

    // ---------------- listings & reports ----------------
    @Transactional(readOnly = true)
    public PageDto<AdminListingDto> listings(String q, int page, int size) {
        Page<ResaleListing> p = listings.adminSearch(CatalogService.norm(q), pg(page, size, Sort.by("id").descending()));
        return new PageDto<>(p.getContent().stream().map(l -> new AdminListingDto(l.getId(), l.getTitle(), l.getSeller().getEmail(), l.getPrice(),
                l.getStatus().name(), l.getCreatedAt())).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional
    public AdminListingDto moderateListing(User admin, Long id, ListingStatus to) {
        ResaleListing l = listings.findById(id).orElseThrow(() -> ApiException.notFound("Listing"));
        OwnedProduct op = l.getOwnedProduct();
        if (to == ListingStatus.PAUSED) {
            if (listings.transition(id, ListingStatus.PUBLISHED, ListingStatus.PAUSED) == 0) throw ApiException.conflict("Only published listings can be taken down");
            op.setStatus(OwnedStatus.ACTIVE);
        } else if (to == ListingStatus.PUBLISHED) {
            if (listings.transition(id, ListingStatus.PAUSED, ListingStatus.PUBLISHED) == 0) throw ApiException.conflict("Only paused listings can be re-published");
            op.setStatus(OwnedStatus.LISTED);
        } else {
            throw ApiException.badRequest("Moderation can only pause or re-publish a listing");
        }
        owned.save(op);
        audit.log(admin.getId(), "LISTING_" + to, "ResaleListing", id, l.getTitle());
        ResaleListing n = listings.findById(id).orElseThrow();
        return new AdminListingDto(n.getId(), n.getTitle(), n.getSeller().getEmail(), n.getPrice(), n.getStatus().name(), n.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageDto<ReportDto> reports(boolean openOnly, int page, int size) {
        Pageable p = pg(page, size, Sort.by("id").descending());
        Page<ListingReport> r = openOnly ? reports.findByStatus(ReportStatus.OPEN, p) : reports.findAll(p);
        return new PageDto<>(r.getContent().stream().map(x -> new ReportDto(x.getId(), x.getListing().getId(), x.getListing().getTitle(),
                x.getReason(), x.getStatus().name(), x.getCreatedAt())).toList(), r.getNumber(), r.getSize(), r.getTotalElements(), r.getTotalPages());
    }

    @Transactional
    public void resolveReport(User admin, Long id) {
        ListingReport r = reports.findById(id).orElseThrow(() -> ApiException.notFound("Report"));
        r.setStatus(ReportStatus.RESOLVED);
        reports.save(r);
        audit.log(admin.getId(), "REPORT_RESOLVED", "ListingReport", id, null);
    }

    // ---------------- reviews ----------------
    @Transactional(readOnly = true)
    public PageDto<AdminReviewDto> reviews(String q, int page, int size) {
        Page<Review> p = reviews.search(CatalogService.norm(q), pg(page, size, Sort.by("id").descending()));
        return new PageDto<>(p.getContent().stream().map(r -> new AdminReviewDto(r.getId(), r.getProduct().getName(), r.getUser().getFullName(),
                r.getRating(), r.getComment(), r.isHidden())).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional
    public void setReviewHidden(User admin, Long id, boolean hidden) {
        Review r = reviews.findById(id).orElseThrow(() -> ApiException.notFound("Review"));
        r.setHidden(hidden);
        reviews.save(r);
        audit.log(admin.getId(), hidden ? "REVIEW_HIDDEN" : "REVIEW_RESTORED", "Review", id, null);
    }

    // ---------------- lifecycle oversight (read-only) & audit ----------------
    @Transactional(readOnly = true)
    public PageDto<AdminEventDto> events(String q, int page, int size) {
        Page<LifecycleEvent> p = events.search(CatalogService.norm(q), pg(page, size, Sort.by("id").descending()));
        return new PageDto<>(p.getContent().stream().map(e -> new AdminEventDto(e.getId(), e.getOwnedProduct().getId(), e.getOwnedProduct().getName(),
                e.getEventType().name(), e.getVerification().name(), e.getActorLabel(), e.getDetails(), e.getCreatedAt())).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PageDto<ActivityDto> auditLog(int page, int size) {
        Page<AuditLog> p = auditRepo.findAll(pg(page, size, Sort.by("id").descending()));
        return new PageDto<>(p.getContent().stream().map(a -> new ActivityDto(a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt())).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
