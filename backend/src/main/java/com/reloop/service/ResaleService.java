package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resale marketplace.
 * Purchase flow: buy() atomically RESERVES the listing (conditional UPDATE ... WHERE status='PUBLISHED'), so two buyers
 * cannot both win. Ownership moves ONLY in completeTransfer(), which runs after a confirmed payment:
 *   - Razorpay: after server-side signature verification, or
 *   - Cash on Delivery: after the SELLER confirms handover/payment received.
 * A frontend "success" message never transfers ownership.
 */
@Service
@RequiredArgsConstructor
public class ResaleService {
    private static final Set<ListingStatus> BLOCKING = EnumSet.of(ListingStatus.DRAFT, ListingStatus.PUBLISHED, ListingStatus.PAUSED, ListingStatus.RESERVED);

    private final ResaleListingRepository listings;
    private final OwnedProductRepository owned;
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final AddressRepository addresses;
    private final OwnershipTransferRepository transfers;
    private final ListingReportRepository reports;
    private final UserRepository users;
    private final LifecycleService lifecycle;
    private final PaymentService payments;
    private final OrderService orderService;
    private final StorageService storage;
    private final AuditService audit;
    private final RepairRequestRepository repairs;

    // ---------------- browsing ----------------
    @Transactional(readOnly = true)
    public PageDto<ListingDto> browse(String q, ItemCondition cond, BigDecimal min, BigDecimal max, String sort, int page, int size) {
        Sort s = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };
        Page<ResaleListing> p = listings.searchPublic(ListingStatus.PUBLISHED, CatalogService.norm(q), cond, min, max,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48), s));
        return new PageDto<>(p.getContent().stream().map(l -> toDto(l, null, false)).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ListingDto detail(Long id, User viewer) {
        ResaleListing l = listings.findById(id).orElseThrow(() -> ApiException.notFound("Listing"));
        boolean mine = viewer != null && l.getSeller().getId().equals(viewer.getId());
        boolean visible = l.getStatus() == ListingStatus.PUBLISHED || l.getStatus() == ListingStatus.RESERVED || l.getStatus() == ListingStatus.SOLD;
        if (!visible && !mine) throw ApiException.notFound("Listing");
        return toDto(l, viewer, true);
    }

    @Transactional(readOnly = true)
    public List<ListingDto> mine(Long userId) {
        return listings.findBySellerIdOrderByCreatedAtDesc(userId).stream().map(l -> toDto(l, users.getReferenceById(userId), false)).toList();
    }

    /** Reservations awaiting the seller's handover confirmation (COD) or payment (online). */
    @Transactional(readOnly = true)
    public List<SaleDto> pendingSales(Long sellerId) {
        return orderItems.findSales(sellerId, OrderType.RESALE, EnumSet.of(OrderStatus.PLACED, OrderStatus.PENDING_PAYMENT)).stream()
                .map(i -> new SaleDto(i.getOrderRef().getId(), i.getOrderRef().getOrderNumber(), i.getListing().getId(), i.getProductName(),
                        CatalogService.firstName(i.getOrderRef().getUser().getFullName()), i.getUnitPrice(),
                        i.getOrderRef().getPaymentMethod().name(), i.getOrderRef().getStatus().name())).toList();
    }

    // ---------------- seller actions ----------------
    @Transactional
    public ListingDto create(User seller, ListingRequest r) {
        OwnedProduct op = owned.findByIdAndOwnerId(r.ownedProductId(), seller.getId()).orElseThrow(() -> ApiException.notFound("Product"));
        if (op.getStatus() != OwnedStatus.ACTIVE) throw ApiException.conflict("Only active products can be listed for resale");
        if (listings.existsByOwnedProductIdAndStatusIn(op.getId(), BLOCKING)) throw ApiException.conflict("This product already has an open listing");
        if (repairs.countByOwnedProductIdAndStatusIn(op.getId(), LifecycleService.OPEN_REPAIRS) > 0)
            throw ApiException.conflict("Finish or cancel the open repair before listing this product");
        ResaleListing l = new ResaleListing();
        l.setOwnedProduct(op);
        l.setSeller(users.getReferenceById(seller.getId()));
        l.setTitle(r.title().trim());
        l.setDescription(r.description());
        l.setItemCondition(r.condition());
        l.setPrice(r.price());
        l.setImageUrl(storage.requireTrusted(r.imageUrl()));
        l.setStatus(ListingStatus.DRAFT);
        return toDto(listings.save(l), seller, false);
    }

    @Transactional
    public ListingDto update(User seller, Long id, ListingUpdateRequest r) {
        ResaleListing l = ownListing(seller, id);
        if (l.getStatus() != ListingStatus.DRAFT && l.getStatus() != ListingStatus.PAUSED)
            throw ApiException.conflict("Pause the listing before editing it");
        l.setTitle(r.title().trim());
        l.setDescription(r.description());
        l.setItemCondition(r.condition());
        l.setPrice(r.price());
        l.setImageUrl(storage.requireTrusted(r.imageUrl()));
        return toDto(listings.save(l), seller, false);
    }

    @Transactional
    public ListingDto publish(User seller, Long id) {
        ResaleListing l = ownListing(seller, id);
        boolean ok = listings.transition(id, ListingStatus.DRAFT, ListingStatus.PUBLISHED) == 1
                || listings.transition(id, ListingStatus.PAUSED, ListingStatus.PUBLISHED) == 1;
        if (!ok) throw ApiException.conflict("Only draft or paused listings can be published");
        OwnedProduct op = l.getOwnedProduct();
        op.setStatus(OwnedStatus.LISTED);
        owned.save(op);
        lifecycle.record(op, EventType.RESALE_LISTED, Verification.SELF_REPORTED, seller.getId(), "Owner", "Listed for resale on the ReLoop marketplace");
        return toDto(listings.findById(id).orElseThrow(), seller, false);
    }

    @Transactional
    public ListingDto pause(User seller, Long id) {
        ResaleListing l = ownListing(seller, id);
        if (listings.transition(id, ListingStatus.PUBLISHED, ListingStatus.PAUSED) == 0)
            throw ApiException.conflict("Only published listings can be paused");
        OwnedProduct op = l.getOwnedProduct();
        op.setStatus(OwnedStatus.ACTIVE);
        owned.save(op);
        return toDto(listings.findById(id).orElseThrow(), seller, false);
    }

    /** Marks a listing sold OUTSIDE the platform. This does NOT transfer ownership in ReLoop. */
    @Transactional
    public ListingDto markSoldExternally(User seller, Long id) {
        ResaleListing l = ownListing(seller, id);
        boolean ok = listings.transition(id, ListingStatus.PUBLISHED, ListingStatus.SOLD) == 1
                || listings.transition(id, ListingStatus.PAUSED, ListingStatus.SOLD) == 1;
        if (!ok) throw ApiException.conflict("Only published or paused listings can be marked sold");
        OwnedProduct op = l.getOwnedProduct();
        op.setStatus(OwnedStatus.ACTIVE);
        owned.save(op);
        audit.log(seller.getId(), "LISTING_SOLD_EXTERNALLY", "ResaleListing", id, null);
        return toDto(listings.findById(id).orElseThrow(), seller, false);
    }

    private ResaleListing ownListing(User seller, Long id) {
        return listings.findById(id).filter(l -> l.getSeller().getId().equals(seller.getId())).orElseThrow(() -> ApiException.notFound("Listing"));
    }

    // ---------------- buying ----------------
    @Transactional
    public OrderDto buy(User buyer, Long listingId, CheckoutRequest r) {
        ResaleListing l = listings.findById(listingId).orElseThrow(() -> ApiException.notFound("Listing"));
        if (l.getSeller().getId().equals(buyer.getId())) throw ApiException.badRequest("You cannot buy your own listing");
        Address addr = addresses.findByIdAndUserIdAndDeletedFalse(r.addressId(), buyer.getId())
                .orElseThrow(() -> ApiException.badRequest("Please select a valid delivery address"));
        if (r.paymentMethod() == PaymentMethod.RAZORPAY && !payments.enabled())
            throw ApiException.badRequest("Online payments are not configured. Please choose Cash on Delivery.");
        // Atomic reservation: exactly one concurrent buyer gets affected-rows == 1.
        if (listings.reserve(listingId, ListingStatus.PUBLISHED, ListingStatus.RESERVED, buyer.getId()) == 0)
            throw ApiException.conflict("Sorry, this item is no longer available");
        if (!l.getOwnedProduct().getOwner().getId().equals(l.getSeller().getId()))
            throw ApiException.conflict("This listing is no longer valid");
        CustomerOrder o = new CustomerOrder();
        o.setUser(users.getReferenceById(buyer.getId()));
        o.setOrderNumber(OrderService.newOrderNumber("RS"));
        o.setType(OrderType.RESALE);
        o.setPaymentMethod(r.paymentMethod());
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setStatus(r.paymentMethod() == PaymentMethod.COD ? OrderStatus.PLACED : OrderStatus.PENDING_PAYMENT);
        o.setSubtotal(l.getPrice());
        o.setTotal(l.getPrice());
        o.setShipName(buyer.getFullName());
        o.setShipAddress(OrderService.formatAddress(addr));
        o.setShipPhone(addr.getPhone());
        o = orders.save(o);
        OrderItem oi = new OrderItem();
        oi.setOrderRef(o);
        oi.setListing(l);
        oi.setProductName(l.getTitle());
        oi.setUnitPrice(l.getPrice());
        oi.setQuantity(1);
        orderItems.save(oi);
        String gateway = r.paymentMethod() == PaymentMethod.RAZORPAY ? payments.createGatewayOrder(o) : null;
        audit.log(buyer.getId(), "RESALE_RESERVED", "ResaleListing", listingId, o.getOrderNumber());
        return orderService.toDto(o, gateway);
    }

    /** Razorpay path: verify signature, then complete the transfer, atomically. */
    @Transactional
    public OrderDto verifyPaymentAndTransfer(User buyer, RazorpayVerifyRequest r) {
        CustomerOrder o = payments.verify(buyer.getId(), r);
        if (o.getType() != OrderType.RESALE) throw ApiException.badRequest("Not a marketplace order");
        if (!transfers.existsByOrderId(o.getId())) completeTransfer(o, "payment verified via Razorpay");
        return orderService.toDto(orders.findById(o.getId()).orElseThrow(), null);
    }

    /** COD path: the seller confirms that the buyer received the item and paid. */
    @Transactional
    public OrderDto confirmHandover(User seller, Long orderId) {
        CustomerOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        ResaleListing l = listingOf(o);
        if (o.getType() != OrderType.RESALE || !l.getSeller().getId().equals(seller.getId())) throw ApiException.notFound("Order");
        if (o.getPaymentMethod() != PaymentMethod.COD) throw ApiException.badRequest("Online-paid orders transfer automatically after payment verification");
        if (o.getStatus() != OrderStatus.PLACED) throw ApiException.conflict("This order is not awaiting handover");
        completeTransfer(o, "seller confirmed handover and payment");
        return orderService.toDto(orders.findById(orderId).orElseThrow(), null);
    }

    @Transactional
    public OrderDto cancelReservation(User actor, Long orderId) {
        CustomerOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        ResaleListing l = listingOf(o);
        boolean isBuyer = o.getUser().getId().equals(actor.getId());
        boolean isSeller = l.getSeller().getId().equals(actor.getId());
        if (o.getType() != OrderType.RESALE || (!isBuyer && !isSeller)) throw ApiException.notFound("Order");
        if (o.getPaymentStatus() == PaymentStatus.PAID) throw ApiException.conflict("Paid orders cannot be cancelled here. Contact support for a refund.");
        if (orders.transition(orderId, EnumSet.of(OrderStatus.PLACED, OrderStatus.PENDING_PAYMENT), OrderStatus.CANCELLED) == 0)
            throw ApiException.conflict("This order can no longer be cancelled");
        listings.transition(l.getId(), ListingStatus.RESERVED, ListingStatus.PUBLISHED);
        audit.log(actor.getId(), "RESALE_RESERVATION_CANCELLED", "Order", orderId, null);
        return orderService.toDto(orders.findById(orderId).orElseThrow(), null);
    }

    /** The single place where ownership changes. Everything below commits or rolls back together. */
    private void completeTransfer(CustomerOrder o, String how) {
        ResaleListing l = listingOf(o);
        OwnedProduct op = l.getOwnedProduct();
        User seller = l.getSeller();
        if (!op.getOwner().getId().equals(seller.getId())) throw ApiException.conflict("Seller is no longer the owner of this product");
        if (listings.transition(l.getId(), ListingStatus.RESERVED, ListingStatus.SOLD) == 0)
            throw ApiException.conflict("Listing is not reserved");
        OwnershipTransfer t = new OwnershipTransfer();
        t.setOwnedProduct(op);
        t.setFromUser(seller);
        t.setToUser(o.getUser());
        t.setListingId(l.getId());
        t.setOrderId(o.getId());
        transfers.save(t);
        op.setOwner(o.getUser());
        op.setStatus(OwnedStatus.ACTIVE);
        op.setReceiptFile(null); // the previous owner's private receipt is not handed to the buyer
        owned.save(op);
        CustomerOrder fresh = orders.findById(o.getId()).orElseThrow();
        fresh.setStatus(OrderStatus.DELIVERED);
        fresh.setPaymentStatus(PaymentStatus.PAID);
        orders.save(fresh);
        lifecycle.record(op, EventType.OWNERSHIP_TRANSFERRED, Verification.SELF_REPORTED, null, "System",
                "Ownership transferred through a ReLoop marketplace sale (" + how + ")");
        audit.log(null, "OWNERSHIP_TRANSFERRED", "OwnedProduct", op.getId(), "order " + o.getOrderNumber());
    }

    private ResaleListing listingOf(CustomerOrder o) {
        return orderItems.findByOrderRefId(o.getId()).stream().map(OrderItem::getListing).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> ApiException.notFound("Listing"));
    }

    @Transactional
    public void report(User reporter, Long listingId, String reason) {
        ResaleListing l = listings.findById(listingId).orElseThrow(() -> ApiException.notFound("Listing"));
        if (l.getSeller().getId().equals(reporter.getId())) throw ApiException.badRequest("You cannot report your own listing");
        if (reports.existsByListingIdAndReporterId(listingId, reporter.getId())) throw ApiException.conflict("You have already reported this listing");
        ListingReport rp = new ListingReport();
        rp.setListing(l);
        rp.setReporter(users.getReferenceById(reporter.getId()));
        rp.setReason(reason.trim());
        reports.save(rp);
    }

    // ---------------- mapping ----------------
    /** Buyers get only safe fields: first name of seller, sanitised history. Never emails, receipts or serials. */
    private ListingDto toDto(ResaleListing l, User viewer, boolean withHistory) {
        OwnedProduct op = l.getOwnedProduct();
        boolean mine = viewer != null && l.getSeller().getId().equals(viewer.getId());
        return new ListingDto(l.getId(), l.getTitle(), l.getDescription(), l.getItemCondition().name(), l.getPrice(), l.getImageUrl(),
                l.getStatus().name(), CatalogService.firstName(l.getSeller().getFullName()), op.getName(), op.getCategory(), op.getBrand(),
                lifecycle.warrantyStatus(op.getId()), withHistory ? lifecycle.publicEvents(op.getId()) : List.of(),
                mine ? op.getId() : null, mine);
    }
}
