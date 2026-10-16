package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Prices and totals are ALWAYS recomputed from the database; the client never supplies them. */
@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;
    private final InventoryRepository inventory;
    private final UserRepository users;

    @Transactional
    public Cart getOrCreate(Long userId) {
        return carts.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(users.getReferenceById(userId));
            return carts.save(c);
        });
    }

    @Transactional
    public CartDto view(Long userId) {
        Cart cart = getOrCreate(userId);
        List<CartItem> list = items.findByCartIdOrderByIdAsc(cart.getId());
        BigDecimal subtotal = BigDecimal.ZERO;
        int count = 0;
        boolean unavailable = false;
        var dtos = new java.util.ArrayList<CartItemDto>();
        for (CartItem ci : list) {
            Product p = ci.getProduct();
            int stock = inventory.findByProductId(p.getId()).map(Inventory::getQuantity).orElse(0);
            boolean ok = p.isActive() && stock >= ci.getQuantity();
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            dtos.add(new CartItemDto(ci.getId(), p.getId(), p.getName(), p.getImageUrl(), p.getPrice(), ci.getQuantity(), stock, ok, line));
            count += ci.getQuantity();
            if (ok) subtotal = subtotal.add(line); else unavailable = true;
        }
        return new CartDto(dtos, count, subtotal, unavailable);
    }

    @Transactional
    public CartDto add(Long userId, CartAddRequest r) {
        Cart cart = getOrCreate(userId);
        Product p = products.findById(r.productId()).filter(Product::isActive).orElseThrow(() -> ApiException.notFound("Product"));
        int existing = items.findByCartIdAndProductId(cart.getId(), p.getId()).map(CartItem::getQuantity).orElse(0);
        int wanted = existing + r.quantity();
        checkStock(p.getId(), wanted);
        CartItem ci = items.findByCartIdAndProductId(cart.getId(), p.getId()).orElseGet(() -> {
            CartItem n = new CartItem();
            n.setCart(cart);
            n.setProduct(p);
            return n;
        });
        ci.setQuantity(wanted);
        items.save(ci);
        return view(userId);
    }

    @Transactional
    public CartDto setQuantity(Long userId, Long itemId, int qty) {
        Cart cart = getOrCreate(userId);
        CartItem ci = items.findByIdAndCartId(itemId, cart.getId()).orElseThrow(() -> ApiException.notFound("Cart item"));
        checkStock(ci.getProduct().getId(), qty);
        ci.setQuantity(qty);
        items.save(ci);
        return view(userId);
    }

    @Transactional
    public CartDto remove(Long userId, Long itemId) {
        Cart cart = getOrCreate(userId);
        items.findByIdAndCartId(itemId, cart.getId()).ifPresent(items::delete);
        return view(userId);
    }

    @Transactional
    public CartDto clear(Long userId) {
        Cart cart = getOrCreate(userId);
        items.deleteByCartId(cart.getId());
        return view(userId);
    }

    private void checkStock(Long productId, int wanted) {
        int stock = inventory.findByProductId(productId).map(Inventory::getQuantity).orElse(0);
        if (stock <= 0) throw ApiException.conflict("This product is out of stock");
        if (wanted > stock) throw ApiException.conflict("Only " + stock + " left in stock");
    }
}
