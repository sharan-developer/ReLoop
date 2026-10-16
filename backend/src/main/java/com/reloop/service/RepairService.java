package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repair workflow. Every status change goes through {@link #transition}, which enforces the allowed-transition map.
 * Role rules (who may trigger which change) are enforced in the public methods.
 */
@Service
@RequiredArgsConstructor
public class RepairService {
    private final RepairRequestRepository requests;
    private final RepairImageRepository repairImages;
    private final RepairQuotationRepository quotes;
    private final RepairStatusHistoryRepository history;
    private final OwnedProductRepository owned;
    private final PartnerProfileRepository partners;
    private final UserRepository users;
    private final LifecycleService lifecycle;
    private final StorageService storage;

    static final Map<RepairStatus, Set<RepairStatus>> ALLOWED = Map.of(
            RepairStatus.REQUESTED, EnumSet.of(RepairStatus.REVIEWED, RepairStatus.QUOTED, RepairStatus.REJECTED, RepairStatus.CANCELLED),
            RepairStatus.REVIEWED, EnumSet.of(RepairStatus.QUOTED, RepairStatus.REJECTED, RepairStatus.CANCELLED),
            RepairStatus.QUOTED, EnumSet.of(RepairStatus.APPROVED, RepairStatus.REVIEWED, RepairStatus.REJECTED, RepairStatus.CANCELLED),
            RepairStatus.APPROVED, EnumSet.of(RepairStatus.IN_PROGRESS, RepairStatus.CANCELLED),
            RepairStatus.IN_PROGRESS, EnumSet.of(RepairStatus.COMPLETED));

    // ---------------- customer ----------------
    @Transactional
    public RepairDto create(User customer, RepairCreateRequest r) {
        OwnedProduct op = owned.findByIdAndOwnerId(r.ownedProductId(), customer.getId()).orElseThrow(() -> ApiException.notFound("Product"));
        if (op.getStatus() != OwnedStatus.ACTIVE) throw ApiException.conflict("Repairs can only be requested for active products that are not listed for sale");
        PartnerProfile pp = partners.findById(r.partnerId()).filter(PartnerProfile::isApproved).orElseThrow(() -> ApiException.badRequest("Select an approved repair partner"));
        if (requests.countByOwnedProductIdAndStatusIn(op.getId(), LifecycleService.OPEN_REPAIRS) > 0)
            throw ApiException.conflict("This product already has an open repair request");
        RepairRequest rr = new RepairRequest();
        rr.setOwnedProduct(op);
        rr.setCustomer(users.getReferenceById(customer.getId()));
        rr.setPartner(pp);
        rr.setProblem(r.problem().trim());
        rr.setStatus(RepairStatus.REQUESTED);
        rr = requests.save(rr);
        if (r.imageUrls() != null) {
            for (String url : r.imageUrls()) {
                if (url == null || url.isBlank()) continue;
                RepairImage ri = new RepairImage();
                ri.setRepairRequest(rr);
                ri.setUrl(storage.requireTrusted(url));
                repairImages.save(ri);
            }
        }
        addHistory(rr, null, RepairStatus.REQUESTED, customer.getId(), "Repair requested");
        lifecycle.record(op, EventType.REPAIR_REQUESTED, Verification.SELF_REPORTED, customer.getId(), "Owner",
                "Repair requested from " + pp.getBusinessName() + ": " + rr.getProblem());
        return toDto(rr);
    }

    @Transactional(readOnly = true)
    public List<RepairDto> listForCustomer(Long userId) {
        return requests.findByCustomerIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RepairDto getForCustomer(Long userId, Long id) { return toDto(customerRequest(userId, id)); }

    @Transactional
    public RepairDto acceptQuote(User customer, Long id, Long quoteId) {
        RepairRequest rr = customerRequest(customer.getId(), id);
        RepairQuotation q = quotes.findById(quoteId).filter(x -> x.getRepairRequest().getId().equals(id)).orElseThrow(() -> ApiException.notFound("Quotation"));
        if (q.getStatus() != QuoteStatus.PENDING) throw ApiException.conflict("This quotation is no longer open");
        transition(rr, RepairStatus.APPROVED, customer.getId(), "Quotation of " + q.getAmount() + " accepted");
        q.setStatus(QuoteStatus.ACCEPTED);
        quotes.save(q);
        lifecycle.record(rr.getOwnedProduct(), EventType.REPAIR_APPROVED, Verification.SELF_REPORTED, customer.getId(), "Owner",
                "Owner approved repair quotation of INR " + q.getAmount());
        return toDto(rr);
    }

    @Transactional
    public RepairDto rejectQuote(User customer, Long id, Long quoteId) {
        RepairRequest rr = customerRequest(customer.getId(), id);
        RepairQuotation q = quotes.findById(quoteId).filter(x -> x.getRepairRequest().getId().equals(id)).orElseThrow(() -> ApiException.notFound("Quotation"));
        if (q.getStatus() != QuoteStatus.PENDING) throw ApiException.conflict("This quotation is no longer open");
        transition(rr, RepairStatus.REVIEWED, customer.getId(), "Quotation rejected by customer");
        q.setStatus(QuoteStatus.REJECTED);
        quotes.save(q);
        return toDto(rr);
    }

    @Transactional
    public RepairDto cancel(User customer, Long id) {
        RepairRequest rr = customerRequest(customer.getId(), id);
        transition(rr, RepairStatus.CANCELLED, customer.getId(), "Cancelled by customer");
        return toDto(rr);
    }

    private RepairRequest customerRequest(Long userId, Long id) {
        return requests.findById(id).filter(r -> r.getCustomer().getId().equals(userId)).orElseThrow(() -> ApiException.notFound("Repair request"));
    }

    // ---------------- partner ----------------
    private PartnerProfile approvedPartner(Long userId) {
        return partners.findByUserId(userId).filter(PartnerProfile::isApproved).orElseThrow(() -> ApiException.forbidden("Repair partner account is not approved"));
    }

    /** A partner can only ever see/modify requests assigned to their own profile. */
    private RepairRequest partnerRequest(User u, Long id) {
        PartnerProfile pp = approvedPartner(u.getId());
        return requests.findById(id).filter(r -> r.getPartner().getId().equals(pp.getId())).orElseThrow(() -> ApiException.notFound("Repair request"));
    }

    @Transactional(readOnly = true)
    public List<RepairDto> listForPartner(User u) {
        PartnerProfile pp = approvedPartner(u.getId());
        return requests.findByPartnerIdOrderByCreatedAtDesc(pp.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RepairDto getForPartner(User u, Long id) { return toDto(partnerRequest(u, id)); }

    @Transactional
    public RepairDto review(User u, Long id, String note) {
        RepairRequest rr = partnerRequest(u, id);
        transition(rr, RepairStatus.REVIEWED, u.getId(), note == null || note.isBlank() ? "Reviewed by repair partner" : note);
        return toDto(rr);
    }

    @Transactional
    public RepairDto quote(User u, Long id, QuoteRequest r) {
        RepairRequest rr = partnerRequest(u, id);
        transition(rr, RepairStatus.QUOTED, u.getId(), "Quotation submitted: INR " + r.amount());
        RepairQuotation q = new RepairQuotation();
        q.setRepairRequest(rr);
        q.setAmount(r.amount());
        q.setEstimatedDays(r.estimatedDays());
        q.setNotes(r.notes());
        quotes.save(q);
        return toDto(rr);
    }

    @Transactional
    public RepairDto reject(User u, Long id, String note) {
        RepairRequest rr = partnerRequest(u, id);
        transition(rr, RepairStatus.REJECTED, u.getId(), note == null || note.isBlank() ? "Declined by repair partner" : note);
        return toDto(rr);
    }

    @Transactional
    public RepairDto start(User u, Long id) {
        RepairRequest rr = partnerRequest(u, id);
        transition(rr, RepairStatus.IN_PROGRESS, u.getId(), "Repair started");
        lifecycle.record(rr.getOwnedProduct(), EventType.REPAIR_STARTED, Verification.PARTNER_CONFIRMED, u.getId(),
                "Repair partner: " + rr.getPartner().getBusinessName(), "Repair work started");
        return toDto(rr);
    }

    @Transactional
    public RepairDto complete(User u, Long id, CompleteRepairRequest r) {
        RepairRequest rr = partnerRequest(u, id);
        transition(rr, RepairStatus.COMPLETED, u.getId(), "Repair completed");
        rr.setWorkNotes(r.workNotes().trim());
        rr.setFinalCost(r.finalCost());
        requests.save(rr);
        lifecycle.record(rr.getOwnedProduct(), EventType.REPAIR_COMPLETED, Verification.PARTNER_CONFIRMED, u.getId(),
                "Repair partner: " + rr.getPartner().getBusinessName(),
                "Repair completed. Work performed: " + rr.getWorkNotes() + " (final cost INR " + r.finalCost() + ")");
        return toDto(rr);
    }

    // ---------------- core ----------------
    private void transition(RepairRequest rr, RepairStatus to, Long actorId, String note) {
        RepairStatus from = rr.getStatus();
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to))
            throw ApiException.conflict("Cannot change a repair from " + from + " to " + to);
        rr.setStatus(to);
        rr.setUpdatedAt(Instant.now());
        requests.save(rr);
        addHistory(rr, from, to, actorId, note);
    }

    private void addHistory(RepairRequest rr, RepairStatus from, RepairStatus to, Long actorId, String note) {
        RepairStatusHistory h = new RepairStatusHistory();
        h.setRepairRequest(rr);
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setActorId(actorId);
        h.setNote(note != null && note.length() > 500 ? note.substring(0, 500) : note);
        history.save(h);
    }

    private RepairDto toDto(RepairRequest rr) {
        return new RepairDto(rr.getId(), rr.getOwnedProduct().getId(), rr.getOwnedProduct().getName(), rr.getCustomer().getFullName(),
                rr.getPartner().getBusinessName(), rr.getProblem(), rr.getStatus().name(),
                repairImages.findByRepairRequestId(rr.getId()).stream().map(RepairImage::getUrl).toList(),
                quotes.findByRepairRequestIdOrderByCreatedAtDesc(rr.getId()).stream()
                        .map(q -> new QuoteDto(q.getId(), q.getAmount(), q.getEstimatedDays(), q.getNotes(), q.getStatus().name(), q.getCreatedAt())).toList(),
                history.findByRepairRequestIdOrderByCreatedAtAscIdAsc(rr.getId()).stream()
                        .map(h -> new RepairHistoryDto(h.getFromStatus() == null ? null : h.getFromStatus().name(), h.getToStatus().name(), h.getNote(), h.getCreatedAt())).toList(),
                rr.getFinalCost(), rr.getWorkNotes(), rr.getCreatedAt());
    }
}
