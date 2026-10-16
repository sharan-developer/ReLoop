package com.reloop.service;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.*;
import com.reloop.entity.Enums.*;
import com.reloop.exception.ApiException;
import com.reloop.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final InventoryRepository inventory;
    private final ProductImageRepository images;
    private final ReviewRepository reviews;
    private final OrderItemRepository orderItems;
    private final StorageService storage;
    private final AuditService audit;

    // ---------------- mapping ----------------
    public List<ProductDto> toDtos(List<Product> list) {
        if (list.isEmpty()) return List.of();
        Map<Long, Integer> stock = inventory.findByProductIdIn(list.stream().map(Product::getId).toList()).stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), Inventory::getQuantity));
        return list.stream().map(p -> toDto(p, stock.getOrDefault(p.getId(), 0), false)).toList();
    }

    public ProductDto toDto(Product p) {
        int stock = inventory.findByProductId(p.getId()).map(Inventory::getQuantity).orElse(0);
        return toDto(p, stock, true);
    }

    private ProductDto toDto(Product p, int stock, boolean withImages) {
        List<String> imgs = new ArrayList<>();
        if (p.getImageUrl() != null) imgs.add(p.getImageUrl());
        if (withImages) images.findByProductIdOrderBySortOrderAsc(p.getId()).forEach(i -> imgs.add(i.getUrl()));
        long count = reviews.countByProductIdAndHiddenFalse(p.getId());
        Double avg = count > 0 ? round1(reviews.averageRating(p.getId())) : null;
        Integer discount = null;
        if (p.getOriginalPrice() != null && p.getOriginalPrice().compareTo(p.getPrice()) > 0) {
            discount = p.getOriginalPrice().subtract(p.getPrice()).multiply(BigDecimal.valueOf(100))
                    .divide(p.getOriginalPrice(), 0, RoundingMode.HALF_UP).intValue();
        }
        return new ProductDto(p.getId(), p.getName(), p.getBrand(), p.getDescription(), p.getItemCondition().name(),
                p.getPrice(), p.getOriginalPrice(), discount, p.getImageUrl(), imgs, p.getCategory().getId(),
                p.getCategory().getName(), stock, p.isActive() && stock > 0, p.isActive(), avg, count, p.getSoldCount());
    }

    private Double round1(Double d) { return d == null ? null : Math.round(d * 10.0) / 10.0; }

    // ---------------- public browsing ----------------
    public List<CategoryDto> listCategories() {
        return categories.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.isActive())).toList();
    }

    public PageDto<ProductDto> search(String q, String categorySlug, ItemCondition cond, BigDecimal min, BigDecimal max,
                                      String sort, int page, int size) {
        Long catId = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            var c = categories.findBySlug(categorySlug);
            if (c.isEmpty()) return new PageDto<>(List.of(), 0, size, 0, 0);
            catId = c.get().getId();
        }
        Sort s = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "popular" -> Sort.by("soldCount").descending();
            default -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
        };
        Pageable pg = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48), s);
        Page<Product> res = products.search(norm(q), catId, cond, min, max, pg);
        return new PageDto<>(toDtos(res.getContent()), res.getNumber(), res.getSize(), res.getTotalElements(), res.getTotalPages());
    }

    public ProductDto getActive(Long id) {
        Product p = products.findById(id).filter(Product::isActive).orElseThrow(() -> ApiException.notFound("Product"));
        return toDto(p);
    }

    public List<ProductDto> related(Long id) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        return toDtos(products.findByCategoryIdAndIdNotAndActiveTrue(p.getCategory().getId(), id,
                PageRequest.of(0, 4, Sort.by("soldCount").descending())));
    }

    public List<ProductDto> byIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> limited = ids.stream().limit(12).toList();
        Map<Long, Product> map = products.findByIdInAndActiveTrue(limited).stream().collect(Collectors.toMap(Product::getId, p -> p));
        return toDtos(limited.stream().map(map::get).filter(Objects::nonNull).toList());
    }

    static String norm(String q) { return q == null ? "" : q.trim(); }

    // ---------------- reviews ----------------
    public PageDto<ReviewDto> listReviews(Long productId, int page, int size) {
        Page<Review> p = reviews.findByProductIdAndHiddenFalseOrderByCreatedAtDesc(productId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new PageDto<>(p.getContent().stream().map(r -> new ReviewDto(r.getId(), firstName(r.getUser().getFullName()),
                r.getRating(), r.getComment(), r.getCreatedAt())).toList(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    /** Verified-purchase review: the order item must belong to the caller, match the product and be DELIVERED. */
    @Transactional
    public ReviewDto addReview(User user, Long productId, ReviewRequest r) {
        OrderItem item = orderItems.findByIdAndOrderRefUserId(r.orderItemId(), user.getId())
                .orElseThrow(() -> ApiException.forbidden("You can only review products you have purchased"));
        if (item.getProduct() == null || !item.getProduct().getId().equals(productId))
            throw ApiException.badRequest("Order item does not match this product");
        if (item.getOrderRef().getStatus() != OrderStatus.DELIVERED)
            throw ApiException.badRequest("You can review a product once your order has been delivered");
        if (reviews.existsByOrderItemId(item.getId())) throw ApiException.conflict("You have already reviewed this purchase");
        Review rv = new Review();
        rv.setProduct(item.getProduct());
        rv.setUser(user);
        rv.setOrderItem(item);
        rv.setRating(r.rating());
        rv.setComment(r.comment() == null ? null : r.comment().trim());
        rv = reviews.save(rv);
        return new ReviewDto(rv.getId(), firstName(user.getFullName()), rv.getRating(), rv.getComment(), rv.getCreatedAt());
    }

    static String firstName(String full) {
        String[] parts = full == null ? new String[0] : full.trim().split("\\s+");
        return parts.length == 0 ? "Customer" : parts[0];
    }

    // ---------------- admin: categories ----------------
    public List<CategoryDto> adminCategories() {
        return categories.findAll(Sort.by("name")).stream().map(c -> new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.isActive())).toList();
    }

    @Transactional
    public CategoryDto saveCategory(Long actorId, Long id, CategoryRequest r) {
        Category c = id == null ? new Category() : categories.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
        if (id == null) {
            String slug = slugify(r.name());
            if (slug.isEmpty() || categories.existsBySlug(slug)) throw ApiException.conflict("A category with this name already exists");
            c.setSlug(slug);
        }
        c.setName(r.name().trim());
        if (r.active() != null) c.setActive(r.active());
        c = categories.save(c);
        audit.log(actorId, id == null ? "CATEGORY_CREATED" : "CATEGORY_UPDATED", "Category", c.getId(), c.getName());
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.isActive());
    }

    static String slugify(String s) {
        String n = Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("[^a-z0-9]+", "-");
        return n.replaceAll("(^-+|-+$)", "");
    }

    // ---------------- admin: products ----------------
    public PageDto<ProductDto> adminProducts(String q, int page, int size) {
        Page<Product> p = products.adminSearch(norm(q), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("id").descending()));
        return new PageDto<>(toDtos(p.getContent()), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional
    public ProductDto saveProduct(Long actorId, Long id, ProductRequest r) {
        Product p = id == null ? new Product() : products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        Category c = categories.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("Unknown category"));
        if (r.originalPrice() != null && r.originalPrice().compareTo(r.price()) < 0)
            throw ApiException.badRequest("Original price cannot be lower than the selling price");
        p.setCategory(c);
        p.setName(r.name().trim());
        p.setBrand(r.brand());
        p.setDescription(r.description());
        p.setItemCondition(r.condition());
        p.setPrice(r.price());
        p.setOriginalPrice(r.originalPrice());
        p.setImageUrl(storage.requireTrusted(r.imageUrl()));
        if (r.active() != null) p.setActive(r.active());
        Product saved = products.save(p);
        Inventory inv = inventory.findByProductId(saved.getId()).orElseGet(() -> {
            Inventory i = new Inventory();
            i.setProduct(saved);
            return i;
        });
        inv.setQuantity(r.stock());
        inventory.save(inv);
        audit.log(actorId, id == null ? "PRODUCT_CREATED" : "PRODUCT_UPDATED", "Product", saved.getId(), saved.getName());
        return toDto(saved);
    }

    /** Products referenced by past orders cannot be hard-deleted: they are deactivated instead. */
    @Transactional
    public void deactivateProduct(Long actorId, Long id) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        p.setActive(false);
        products.save(p);
        audit.log(actorId, "PRODUCT_DEACTIVATED", "Product", id, p.getName());
    }
}
