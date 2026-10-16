package com.reloop.controller;

import com.reloop.dto.Dtos.*;
import com.reloop.security.CurrentUser;
import com.reloop.service.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Everything under /api/admin/** requires ROLE_ADMIN (SecurityConfig). The role is read from the database, not the client. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService admin;
    private final CatalogService catalog;
    private final OrderService orders;
    private final OwnedProductService owned;

    @GetMapping("/stats") public AdminStats stats() { return admin.stats(); }

    // users
    @GetMapping("/users")
    public PageDto<AdminUserDto> users(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return admin.users(q, page, size); }
    @PatchMapping("/users/{id}")
    public AdminUserDto updateUser(@PathVariable Long id, @RequestBody AdminUserUpdate r) {
        if (r.enabled() == null) throw com.reloop.exception.ApiException.badRequest("'enabled' is required");
        return admin.setUserEnabled(CurrentUser.get(), id, r.enabled());
    }

    // categories
    @GetMapping("/categories") public List<CategoryDto> categories() { return catalog.adminCategories(); }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest r) { return catalog.saveCategory(CurrentUser.get().getId(), null, r); }
    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) { return catalog.saveCategory(CurrentUser.get().getId(), id, r); }

    // products
    @GetMapping("/products")
    public PageDto<ProductDto> products(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return catalog.adminProducts(q, page, size); }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    public ProductDto createProduct(@Valid @RequestBody ProductRequest r) { return catalog.saveProduct(CurrentUser.get().getId(), null, r); }
    @PutMapping("/products/{id}")
    public ProductDto updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest r) { return catalog.saveProduct(CurrentUser.get().getId(), id, r); }
    @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(@PathVariable Long id) { catalog.deactivateProduct(CurrentUser.get().getId(), id); }

    // orders
    @GetMapping("/orders")
    public PageDto<AdminOrderDto> orders(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return orders.adminList(q, page, size); }
    @PatchMapping("/orders/{id}/status")
    public AdminOrderDto orderStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest r) { return orders.adminUpdateStatus(CurrentUser.get().getId(), id, r.status()); }

    // partners
    @GetMapping("/partners")
    public PageDto<AdminPartnerDto> partners(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return admin.partners(q, page, size); }
    @PostMapping("/partners/{id}/approve") public AdminPartnerDto approve(@PathVariable Long id) { return admin.setPartnerApproved(CurrentUser.get(), id, true); }
    @PostMapping("/partners/{id}/revoke") public AdminPartnerDto revoke(@PathVariable Long id) { return admin.setPartnerApproved(CurrentUser.get(), id, false); }

    // listings & reports
    @GetMapping("/listings")
    public PageDto<AdminListingDto> listings(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return admin.listings(q, page, size); }
    @PatchMapping("/listings/{id}/status")
    public AdminListingDto moderate(@PathVariable Long id, @Valid @RequestBody ListingStatusRequest r) { return admin.moderateListing(CurrentUser.get(), id, r.status()); }
    @GetMapping("/reports")
    public PageDto<ReportDto> reports(@RequestParam(defaultValue = "true") boolean openOnly, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return admin.reports(openOnly, page, size); }
    @PostMapping("/reports/{id}/resolve") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@PathVariable Long id) { admin.resolveReport(CurrentUser.get(), id); }

    // reviews
    @GetMapping("/reviews")
    public PageDto<AdminReviewDto> reviews(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return admin.reviews(q, page, size); }
    @PostMapping("/reviews/{id}/hide") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable Long id) { admin.setReviewHidden(CurrentUser.get(), id, true); }
    @PostMapping("/reviews/{id}/restore") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restore(@PathVariable Long id) { admin.setReviewHidden(CurrentUser.get(), id, false); }

    // lifecycle oversight (read-only) and audit trail
    @GetMapping("/lifecycle")
    public PageDto<AdminEventDto> lifecycle(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) { return admin.events(q, page, size); }
    @PostMapping("/lifecycle/products/{id}/confirm-recycling") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmRecycling(@PathVariable Long id) { owned.adminConfirmRecycling(CurrentUser.get().getId(), id); }
    @GetMapping("/audit")
    public PageDto<ActivityDto> audit(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return admin.auditLog(page, size); }
}
