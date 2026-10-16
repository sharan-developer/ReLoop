package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.repository.*;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The ONLY writer of lifecycle_events. Events are append-only: there is no update or delete method anywhere.
 */
@Service
@RequiredArgsConstructor
public class LifecycleService {
    private final LifecycleEventRepository events;
    private final WarrantyRepository warranties;
    private final RepairRequestRepository repairs;

    public static final EnumSet<RepairStatus> OPEN_REPAIRS =
            EnumSet.of(RepairStatus.REQUESTED, RepairStatus.REVIEWED, RepairStatus.QUOTED, RepairStatus.APPROVED, RepairStatus.IN_PROGRESS);

    public void record(OwnedProduct op, EventType type, Verification v, Long actorId, String actorLabel, String details) {
        LifecycleEvent e = new LifecycleEvent();
        e.setOwnedProduct(op);
        e.setEventType(type);
        e.setVerification(v);
        e.setActorId(actorId);
        e.setActorLabel(actorLabel);
        e.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        events.save(e);
    }

    /** Full detail for the authorised owner. */
    public List<LifecycleEventDto> ownerEvents(Long ownedId) {
        return events.findByOwnedProductIdOrderByCreatedAtAscIdAsc(ownedId).stream()
                .map(e -> new LifecycleEventDto(e.getId(), e.getEventType().name(), e.getVerification().name(),
                        e.getActorLabel(), e.getDetails(), e.getCreatedAt())).toList();
    }

    /** Sanitised view for public passport pages and marketplace buyers: no free-text details, no actor names. */
    public List<LifecycleEventDto> publicEvents(Long ownedId) {
        return events.findByOwnedProductIdOrderByCreatedAtAscIdAsc(ownedId).stream()
                .map(e -> new LifecycleEventDto(e.getId(), e.getEventType().name(), e.getVerification().name(),
                        publicActor(e.getActorLabel()), null, e.getCreatedAt())).toList();
    }

    private String publicActor(String label) {
        if (label == null) return "System";
        if (label.startsWith("Repair partner")) return "Repair partner";
        if (label.startsWith("ReLoop admin")) return "ReLoop admin";
        if (label.startsWith("Owner")) return "Owner";
        return "System";
    }

    public String warrantyStatus(Long ownedId) {
        return warranties.findFirstByOwnedProductIdOrderByEndDateDesc(ownedId)
                .map(w -> w.getEndDate().isBefore(LocalDate.now()) ? "EXPIRED" : "ACTIVE")
                .orElse("NOT_RECORDED");
    }

    public OwnedProductDto toDto(OwnedProduct op) {
        var w = warranties.findFirstByOwnedProductIdOrderByEndDateDesc(op.getId()).orElse(null);
        String ws = w == null ? "NOT_RECORDED" : (w.getEndDate().isBefore(LocalDate.now()) ? "EXPIRED" : "ACTIVE");
        long open = repairs.countByOwnedProductIdAndStatusIn(op.getId(), OPEN_REPAIRS);
        return new OwnedProductDto(op.getId(), op.getName(), op.getCategory(), op.getBrand(), op.getModel(),
                op.getSerialNumber(), op.getPurchaseDate(), op.getStatus().name(), ws,
                w == null ? null : w.getEndDate(), op.getReceiptFile() != null, op.getPublicToken(), open);
    }
}
