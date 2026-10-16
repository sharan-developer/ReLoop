package com.reloop.controller;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.Enums.ItemCondition;
import com.reloop.security.CurrentUser;
import com.reloop.service.ResaleService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MarketplaceController {
    private final ResaleService resale;

    // ---- public browsing ----
    @GetMapping("/api/marketplace/listings")
    public PageDto<ListingDto> browse(@RequestParam(required = false) String q, @RequestParam(required = false) ItemCondition condition,
                                      @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice,
                                      @RequestParam(required = false) String sort,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return resale.browse(q, condition, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/api/marketplace/listings/{id}")
    public ListingDto detail(@PathVariable Long id) { return resale.detail(id, CurrentUser.getOrNull()); }

    // ---- buyer ----
    @PostMapping("/api/marketplace/listings/{id}/buy") @ResponseStatus(HttpStatus.CREATED)
    public OrderDto buy(@PathVariable Long id, @Valid @RequestBody CheckoutRequest r) { return resale.buy(CurrentUser.get(), id, r); }

    @PostMapping("/api/marketplace/listings/{id}/report") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable Long id, @Valid @RequestBody ReportRequest r) { resale.report(CurrentUser.get(), id, r.reason()); }

    @PostMapping("/api/marketplace/orders/{orderId}/cancel")
    public OrderDto cancel(@PathVariable Long orderId) { return resale.cancelReservation(CurrentUser.get(), orderId); }

    // ---- seller ----
    @PostMapping("/api/marketplace/orders/{orderId}/confirm-handover")
    public OrderDto handover(@PathVariable Long orderId) { return resale.confirmHandover(CurrentUser.get(), orderId); }

    @GetMapping("/api/my-sales") public List<SaleDto> sales() { return resale.pendingSales(CurrentUser.get().getId()); }

    @GetMapping("/api/my-listings") public List<ListingDto> mine() { return resale.mine(CurrentUser.get().getId()); }

    @PostMapping("/api/my-listings") @ResponseStatus(HttpStatus.CREATED)
    public ListingDto create(@Valid @RequestBody ListingRequest r) { return resale.create(CurrentUser.get(), r); }

    @PutMapping("/api/my-listings/{id}")
    public ListingDto update(@PathVariable Long id, @Valid @RequestBody ListingUpdateRequest r) { return resale.update(CurrentUser.get(), id, r); }

    @PostMapping("/api/my-listings/{id}/publish") public ListingDto publish(@PathVariable Long id) { return resale.publish(CurrentUser.get(), id); }
    @PostMapping("/api/my-listings/{id}/pause") public ListingDto pause(@PathVariable Long id) { return resale.pause(CurrentUser.get(), id); }
    @PostMapping("/api/my-listings/{id}/mark-sold") public ListingDto sold(@PathVariable Long id) { return resale.markSoldExternally(CurrentUser.get(), id); }
}
