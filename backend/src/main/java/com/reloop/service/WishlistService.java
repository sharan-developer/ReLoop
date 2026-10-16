package com.reloop.service;

import com.reloop.dto.Dtos.ProductDto;
import com.reloop.entity.Product;
import com.reloop.entity.WishlistItem;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistItemRepository wishlist;
    private final ProductRepository products;
    private final UserRepository users;
    private final CatalogService catalog;

    @Transactional(readOnly = true)
    public List<ProductDto> list(Long userId) {
        List<Product> ps = wishlist.findByUserIdOrderByCreatedAtDesc(userId).stream().map(WishlistItem::getProduct).toList();
        return catalog.toDtos(ps);
    }

    @Transactional
    public void add(Long userId, Long productId) {
        Product p = products.findById(productId).filter(Product::isActive).orElseThrow(() -> ApiException.notFound("Product"));
        if (wishlist.existsByUserIdAndProductId(userId, productId)) return;
        WishlistItem w = new WishlistItem();
        w.setUser(users.getReferenceById(userId));
        w.setProduct(p);
        wishlist.save(w);
    }

    @Transactional
    public void remove(Long userId, Long productId) { wishlist.deleteByUserIdAndProductId(userId, productId); }
}
