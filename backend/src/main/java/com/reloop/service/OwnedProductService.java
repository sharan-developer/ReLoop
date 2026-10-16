package com.reloop.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.reloop.config.AppProperties;
import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OwnedProductService {
    private final OwnedProductRepository owned;
    private final WarrantyRepository warranties;
    private final RecyclingRecordRepository recycling;
    private final RepairRequestRepository repairs;
    private final ResaleListingRepository listings;
    private final UserRepository users;
    private final LifecycleService lifecycle;
    private final StorageService storage;
    private final AuditService audit;
    private final AppProperties props;

    private static final String OWNER = "Owner";

    /** Object-level check: a product the caller does not own is reported as "not found" (no existence leak). */
    public OwnedProduct requireOwned(Long id, Long userId) {
        return owned.findByIdAndOwnerId(id, userId).orElseThrow(() -> ApiException.notFound("Product"));
    }

    @Transactional
    public OwnedProductDto register(User user, OwnedProductRequest r) {
        if ((r.warrantyStart() == null) != (r.warrantyEnd() == null)) throw ApiException.badRequest("Provide both warranty start and end dates");
        if (r.warrantyStart() != null && r.warrantyEnd().isBefore(r.warrantyStart())) throw ApiException.badRequest("Warranty end date must be on or after the start date");
        OwnedProduct op = new OwnedProduct();
        op.setOwner(users.getReferenceById(user.getId()));
        op.setName(r.name().trim());
        op.setCategory(r.category().trim());
        op.setBrand(r.brand());
        op.setModel(r.model());
        op.setSerialNumber(r.serialNumber());
        op.setPurchaseDate(r.purchaseDate());
        op.setStatus(OwnedStatus.ACTIVE);
        op.setPublicToken(UUID.randomUUID().toString().replace("-", ""));
        op = owned.save(op);
        lifecycle.record(op, EventType.PRODUCT_REGISTERED, Verification.SELF_REPORTED, user.getId(), OWNER, "Product registered by its owner (self-reported)");
        lifecycle.record(op, EventType.PURCHASE_RECORDED, Verification.SELF_REPORTED, user.getId(), OWNER, "Purchase date recorded: " + r.purchaseDate());
        if (r.warrantyStart() != null) saveWarranty(op, user, r.warrantyStart(), r.warrantyEnd(), r.warrantyProvider(), null);
        return lifecycle.toDto(op);
    }

    public List<OwnedProductDto> list(Long userId) {
        return owned.findByOwnerIdOrderByCreatedAtDesc(userId).stream().map(lifecycle::toDto).toList();
    }

    public OwnedProductDto get(Long id, Long userId) { return lifecycle.toDto(requireOwned(id, userId)); }

    public PassportDto passport(Long id, Long userId) {
        OwnedProduct op = requireOwned(id, userId);
        return new PassportDto(lifecycle.toDto(op), lifecycle.ownerEvents(op.getId()));
    }

    /** Public view: no owner identity, serial number, receipt, or free-text event details. */
    public PublicPassportDto publicPassport(String token) {
        OwnedProduct op = owned.findByPublicToken(token).orElseThrow(() -> ApiException.notFound("Passport"));
        return new PublicPassportDto(op.getName(), op.getCategory(), op.getBrand(), op.getModel(), op.getStatus().name(),
                lifecycle.warrantyStatus(op.getId()), lifecycle.publicEvents(op.getId()),
                "Records marked SELF_REPORTED were entered by the owner and are not independently verified. "
                        + "Only PARTNER_CONFIRMED or ADMIN_CONFIRMED records were confirmed by an authorised party.");
    }

    @Transactional
    public OwnedProductDto setWarranty(User user, Long id, WarrantyRequest r) {
        OwnedProduct op = requireOwned(id, user.getId());
        if (r.endDate().isBefore(r.startDate())) throw ApiException.badRequest("Warranty end date must be on or after the start date");
        if (op.getStatus() == OwnedStatus.RECYCLED) throw ApiException.conflict("This product has been recycled");
        saveWarranty(op, user, r.startDate(), r.endDate(), r.provider(), r.notes());
        return lifecycle.toDto(op);
    }

    private void saveWarranty(OwnedProduct op, User user, java.time.LocalDate s, java.time.LocalDate e, String provider, String notes) {
        Warranty w = new Warranty();
        w.setOwnedProduct(op);
        w.setStartDate(s);
        w.setEndDate(e);
        w.setProvider(provider);
        w.setNotes(notes);
        warranties.save(w);
        lifecycle.record(op, EventType.WARRANTY_UPDATED, Verification.SELF_REPORTED, user.getId(), OWNER,
                "Warranty " + s + " to " + e + (provider == null || provider.isBlank() ? "" : " (" + provider + ")") + " (self-reported)");
    }

    @Transactional
    public OwnedProductDto uploadReceipt(User user, Long id, MultipartFile file) {
        OwnedProduct op = requireOwned(id, user.getId());
        op.setReceiptFile(storage.storePrivate(file));
        owned.save(op);
        return lifecycle.toDto(op);
    }

    public Resource receipt(User user, Long id) {
        OwnedProduct op = requireOwned(id, user.getId());
        if (op.getReceiptFile() == null) throw ApiException.notFound("Receipt");
        return storage.loadPrivate(op.getReceiptFile());
    }

    public String receiptName(User user, Long id) { return requireOwned(id, user.getId()).getReceiptFile(); }

    @Transactional
    public OwnedProductDto recycle(User user, Long id, RecycleRequest r) {
        OwnedProduct op = requireOwned(id, user.getId());
        if (op.getStatus() == OwnedStatus.RECYCLED) throw ApiException.conflict("Already recorded as recycled");
        if (op.getStatus() == OwnedStatus.LISTED) throw ApiException.conflict("Pause or remove the resale listing first");
        if (repairs.countByOwnedProductIdAndStatusIn(op.getId(), LifecycleService.OPEN_REPAIRS) > 0)
            throw ApiException.conflict("Finish or cancel open repair requests first");
        if (r.evidenceUrl() != null && !r.evidenceUrl().isBlank() && !r.evidenceUrl().startsWith("https://"))
            throw ApiException.badRequest("Evidence link must be an https URL");
        RecyclingRecord rr = new RecyclingRecord();
        rr.setOwnedProduct(op);
        rr.setRecycledOn(r.recycledOn());
        rr.setPartnerName(r.partnerName());
        rr.setEvidenceUrl(r.evidenceUrl());
        rr.setStatus(RecyclingStatus.SELF_REPORTED);
        rr.setRecordedBy(user.getId());
        recycling.save(rr);
        op.setStatus(OwnedStatus.RECYCLED);
        owned.save(op);
        lifecycle.record(op, EventType.RECYCLING_RECORDED, Verification.SELF_REPORTED, user.getId(), OWNER,
                "Recycling reported by the owner on " + r.recycledOn() + (r.partnerName() == null || r.partnerName().isBlank() ? "" : " via " + r.partnerName())
                        + ". Not verified; this is not an official recycling certificate.");
        audit.log(user.getId(), "RECYCLING_RECORDED", "OwnedProduct", op.getId(), null);
        return lifecycle.toDto(op);
    }

    /** Admin confirmation upgrades a self-reported recycling record. Appends a new event; history is never edited. */
    @Transactional
    public void adminConfirmRecycling(Long adminId, Long ownedId) {
        OwnedProduct op = owned.findById(ownedId).orElseThrow(() -> ApiException.notFound("Product"));
        List<RecyclingRecord> recs = recycling.findByOwnedProductId(ownedId);
        if (recs.isEmpty()) throw ApiException.badRequest("No recycling record to confirm");
        RecyclingRecord rr = recs.get(recs.size() - 1);
        if (rr.getStatus() == RecyclingStatus.CONFIRMED) throw ApiException.conflict("Already confirmed");
        rr.setStatus(RecyclingStatus.CONFIRMED);
        recycling.save(rr);
        lifecycle.record(op, EventType.RECYCLING_RECORDED, Verification.ADMIN_CONFIRMED, adminId, "ReLoop admin",
                "Recycling record confirmed by a ReLoop administrator after review of supplied evidence.");
        audit.log(adminId, "RECYCLING_CONFIRMED", "OwnedProduct", ownedId, null);
    }

    public byte[] qrPng(String token) {
        owned.findByPublicToken(token).orElseThrow(() -> ApiException.notFound("Passport"));
        String url = props.getPublicBaseUrl().replaceAll("/+$", "") + "/passport/" + token;
        try {
            BitMatrix m = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(m, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate QR code");
        }
    }
}
