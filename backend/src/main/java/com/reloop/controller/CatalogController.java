package com.reloop.controller;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.Enums.ItemCondition;
import com.reloop.security.CurrentUser;
import com.reloop.service.CatalogService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalog;

    @GetMapping("/api/categories")
    public List<CategoryDto> categories() { return catalog.listCategories(); }

    @GetMapping("/api/products")
    public PageDto<ProductDto> search(@RequestParam(required = false) String q, @RequestParam(required = false) String category,
                                      @RequestParam(required = false) ItemCondition condition,
                                      @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice,
                                      @RequestParam(required = false) String sort,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return catalog.search(q, category, condition, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/api/products/by-ids")
    public List<ProductDto> byIds(@RequestParam List<Long> ids) { return catalog.byIds(ids); }

    @GetMapping("/api/products/{id}")
    public ProductDto get(@PathVariable Long id) { return catalog.getActive(id); }

    @GetMapping("/api/products/{id}/related")
    public List<ProductDto> related(@PathVariable Long id) { return catalog.related(id); }

    @GetMapping("/api/products/{id}/reviews")
    public PageDto<ReviewDto> reviews(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return catalog.listReviews(id, page, size);
    }

    @PostMapping("/api/products/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto addReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest r) { return catalog.addReview(CurrentUser.get(), id, r); }
}
