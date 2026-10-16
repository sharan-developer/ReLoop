package com.reloop.controller;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.Enums.OrderType;
import com.reloop.security.CurrentUser;
import com.reloop.service.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Cart, wishlist, orders and payments. All data is scoped to the authenticated user. */
@RestController
@RequiredArgsConstructor
public class ShoppingController {
    private final CartService cart;
    private final WishlistService wishlist;
    private final OrderService orders;
    private final ResaleService resale;
    private final PaymentService payments;

    // ---- cart ----
    @GetMapping("/api/cart") public CartDto cart() { return cart.view(CurrentUser.get().getId()); }
    @PostMapping("/api/cart/items") public CartDto add(@Valid @RequestBody CartAddRequest r) { return cart.add(CurrentUser.get().getId(), r); }
    @PutMapping("/api/cart/items/{id}") public CartDto qty(@PathVariable Long id, @Valid @RequestBody CartQtyRequest r) { return cart.setQuantity(CurrentUser.get().getId(), id, r.quantity()); }
    @DeleteMapping("/api/cart/items/{id}") public CartDto remove(@PathVariable Long id) { return cart.remove(CurrentUser.get().getId(), id); }
    @DeleteMapping("/api/cart") public CartDto clear() { return cart.clear(CurrentUser.get().getId()); }

    // ---- wishlist ----
    @GetMapping("/api/wishlist") public List<ProductDto> wishlist() { return wishlist.list(CurrentUser.get().getId()); }
    @PostMapping("/api/wishlist/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addWish(@PathVariable Long productId) { wishlist.add(CurrentUser.get().getId(), productId); }
    @DeleteMapping("/api/wishlist/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWish(@PathVariable Long productId) { wishlist.remove(CurrentUser.get().getId(), productId); }

    // ---- orders ----
    @PostMapping("/api/orders") @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@Valid @RequestBody CheckoutRequest r) { return orders.checkout(CurrentUser.get(), r); }
    @GetMapping("/api/orders")
    public PageDto<OrderDto> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return orders.list(CurrentUser.get().getId(), page, size); }
    @GetMapping("/api/orders/{id}") public OrderDto get(@PathVariable Long id) { return orders.get(CurrentUser.get().getId(), id); }
    @PostMapping("/api/orders/{id}/cancel") public OrderDto cancel(@PathVariable Long id) { return orders.cancel(CurrentUser.get().getId(), id); }

    // ---- payments ----
    @GetMapping("/api/payments/config") public PaymentConfigDto config() { return payments.config(); }

    @PostMapping("/api/payments/razorpay/verify")
    public OrderDto verify(@Valid @RequestBody RazorpayVerifyRequest r) {
        var user = CurrentUser.get();
        return orders.orderType(user.getId(), r.orderId()) == OrderType.RESALE
                ? resale.verifyPaymentAndTransfer(user, r)
                : orders.verifyStorePayment(user.getId(), r);
    }
}
