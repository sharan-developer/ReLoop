package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final SecureRandom RNG = new SecureRandom();
    static final Set<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PLACED, OrderStatus.CONFIRMED);

    private final CustomerOrderRepository orders;
    private final OrderItemRepository items;
    private final CartItemRepository cartItems;
    private final CartRepository carts;
    private final AddressRepository addresses;
    private final InventoryRepository inventory;
    private final ProductRepository products;
    private final ReviewRepository reviews;
    private final PaymentService payments;
    private final AuditService audit;

    static String newOrderNumber(String prefix) {
        return prefix + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%06d", RNG.nextInt(1_000_000));
    }

    static String formatAddress(Address a) {
        return a.getLine1() + (a.getLine2() == null || a.getLine2().isBlank() ? "" : ", " + a.getLine2())
                + ", " + a.getCity() + ", " + a.getState() + " " + a.getPostalCode();
    }

    /**
     * One transaction: validate -> atomically decrement stock per line -> create order + items (name/price snapshot)
     * -> clear cart. Any failure rolls back every stock decrement.
     */
    @Transactional
    public OrderDto checkout(User user, CheckoutRequest r) {
        Cart cart = carts.findByUserId(user.getId()).orElseThrow(() -> ApiException.badRequest("Your cart is empty"));
        List<CartItem> lines = cartItems.findByCartIdOrderByIdAsc(cart.getId());
        if (lines.isEmpty()) throw ApiException.badRequest("Your cart is empty");
        Address addr = addresses.findByIdAndUserIdAndDeletedFalse(r.addressId(), user.getId())
                .orElseThrow(() -> ApiException.badRequest("Please select a valid delivery address"));
        if (r.paymentMethod() == PaymentMethod.RAZORPAY && !payments.enabled())
            throw ApiException.badRequest("Online payments are not configured. Please choose Cash on Delivery.");

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : lines) {
            Product p = products.findById(ci.getProduct().getId()).orElseThrow();
            if (!p.isActive()) throw ApiException.conflict("'" + p.getName() + "' is no longer available");
            if (inventory.decrement(p.getId(), ci.getQuantity()) == 0)
                throw ApiException.conflict("Not enough stock for '" + p.getName() + "'. Please update your cart.");
            OrderItem oi = new OrderItem();
            oi.setProduct(p);
            oi.setProductName(p.getName());
            oi.setUnitPrice(p.getPrice());
            oi.setQuantity(ci.getQuantity());
            orderItems.add(oi);
            subtotal = subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        CustomerOrder o = new CustomerOrder();
        o.setUser(user);
        o.setOrderNumber(newOrderNumber("RL"));
        o.setType(OrderType.STORE);
        o.setPaymentMethod(r.paymentMethod());
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setStatus(r.paymentMethod() == PaymentMethod.COD ? OrderStatus.PLACED : OrderStatus.PENDING_PAYMENT);
        o.setSubtotal(subtotal);
        o.setTotal(subtotal); // no shipping/tax modelled in this version
        o.setShipName(user.getFullName());
        o.setShipAddress(formatAddress(addr));
        o.setShipPhone(addr.getPhone());
        o = orders.save(o);
        for (OrderItem oi : orderItems) { oi.setOrderRef(o); items.save(oi); }
        String gatewayId = r.paymentMethod() == PaymentMethod.RAZORPAY ? payments.createGatewayOrder(o) : null;
        cartItems.deleteByCartId(cart.getId());
        audit.log(user.getId(), "ORDER_PLACED", "Order", o.getId(), o.getOrderNumber());
        return toDto(o, gatewayId);
    }

    @Transactional(readOnly = true)
    public PageDto<OrderDto> list(Long userId, int page, int size) {
        Page<CustomerOrder> p = orders.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new PageDto<>(p.getContent().stream().map(o -> toDto(o, null)).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long userId, Long id) {
        CustomerOrder o = orders.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Order"));
        return toDto(o, payments.pendingGatewayOrderId(o.getId()));
    }

    /** Customers may cancel until the order ships. Stock is restored exactly once (atomic status transition). */
    @Transactional
    public OrderDto cancel(Long userId, Long id) {
        CustomerOrder o = orders.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Order"));
        if (o.getType() != OrderType.STORE) throw ApiException.badRequest("Marketplace orders are cancelled from the marketplace page");
        cancelAndRestock(o);
        audit.log(userId, "ORDER_CANCELLED", "Order", id, o.getOrderNumber());
        return toDto(orders.findById(id).orElseThrow(), null);
    }

    private void cancelAndRestock(CustomerOrder o) {
        if (orders.transition(o.getId(), CANCELLABLE, OrderStatus.CANCELLED) == 0)
            throw ApiException.conflict("This order can no longer be cancelled");
        for (OrderItem oi : items.findByOrderRefId(o.getId())) {
            if (oi.getProduct() != null) inventory.restock(oi.getProduct().getId(), oi.getQuantity());
        }
    }

    @Transactional(readOnly = true)
    public OrderType orderType(Long userId, Long id) {
        return orders.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Order")).getType();
    }

    /** Store-order payment confirmation (signature verified server-side in PaymentService). */
    @Transactional
    public OrderDto verifyStorePayment(Long userId, RazorpayVerifyRequest r) {
        CustomerOrder o = payments.verify(userId, r);
        audit.log(userId, "PAYMENT_VERIFIED", "Order", o.getId(), o.getOrderNumber());
        return toDto(orders.findById(o.getId()).orElseThrow(), null);
    }

    // ---------------- admin ----------------
    @Transactional(readOnly = true)
    public PageDto<AdminOrderDto> adminList(String q, int page, int size) {
        Page<CustomerOrder> p = orders.search(CatalogService.norm(q), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageDto<>(p.getContent().stream().map(o -> new AdminOrderDto(o.getId(), o.getOrderNumber(), o.getUser().getEmail(),
                o.getStatus().name(), o.getPaymentStatus().name(), o.getTotal(), o.getCreatedAt())).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_FROM = Map.of(
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PLACED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.CONFIRMED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.SHIPPED));

    @Transactional
    public AdminOrderDto adminUpdateStatus(Long actorId, Long id, OrderStatus to) {
        CustomerOrder o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
        if (o.getType() != OrderType.STORE) throw ApiException.badRequest("Marketplace orders are managed through the transfer flow");
        if (to == OrderStatus.CANCELLED) {
            cancelAndRestock(o);
        } else {
            Set<OrderStatus> from = ADMIN_FROM.get(to);
            if (from == null || orders.transition(id, from, to) == 0)
                throw ApiException.badRequest("Invalid status change from " + o.getStatus() + " to " + to);
            if (to == OrderStatus.DELIVERED) {
                CustomerOrder fresh = orders.findById(id).orElseThrow();
                if (fresh.getPaymentMethod() == PaymentMethod.COD) { fresh.setPaymentStatus(PaymentStatus.PAID); orders.save(fresh); }
                for (OrderItem oi : items.findByOrderRefId(id)) if (oi.getProduct() != null) products.addSold(oi.getProduct().getId(), oi.getQuantity());
            }
        }
        audit.log(actorId, "ORDER_STATUS_" + to, "Order", id, o.getOrderNumber());
        CustomerOrder n = orders.findById(id).orElseThrow();
        return new AdminOrderDto(n.getId(), n.getOrderNumber(), n.getUser().getEmail(), n.getStatus().name(), n.getPaymentStatus().name(), n.getTotal(), n.getCreatedAt());
    }

    // ---------------- mapping ----------------
    public OrderDto toDto(CustomerOrder o, String gatewayOrderId) {
        List<OrderItem> its = items.findByOrderRefId(o.getId());
        Set<Long> reviewed = its.isEmpty() ? Set.of() : new HashSet<>(reviews.reviewedItemIds(its.stream().map(OrderItem::getId).toList()));
        boolean delivered = o.getStatus() == OrderStatus.DELIVERED;
        var itemDtos = its.stream().map(i -> new OrderItemDto(i.getId(), i.getProduct() == null ? null : i.getProduct().getId(),
                i.getListing() == null ? null : i.getListing().getId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())), delivered && i.getProduct() != null && !reviewed.contains(i.getId()))).toList();
        boolean razorpay = o.getPaymentMethod() == PaymentMethod.RAZORPAY && o.getStatus() == OrderStatus.PENDING_PAYMENT;
        return new OrderDto(o.getId(), o.getOrderNumber(), o.getStatus().name(), o.getPaymentMethod().name(), o.getPaymentStatus().name(),
                o.getType().name(), o.getSubtotal(), o.getTotal(), o.getCreatedAt(), o.getShipName(), o.getShipAddress(), itemDtos,
                razorpay ? gatewayOrderId : null, razorpay && payments.enabled() ? payments.config().keyId() : null,
                o.getType() == OrderType.STORE && CANCELLABLE.contains(o.getStatus()));
    }
}
