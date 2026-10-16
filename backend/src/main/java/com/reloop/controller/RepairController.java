package com.reloop.controller;

import com.reloop.dto.Dtos.*;
import com.reloop.security.CurrentUser;
import com.reloop.service.RepairService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RepairController {
    private final RepairService repairs;

    // ---- customer ----
    @GetMapping("/api/repairs") public List<RepairDto> mine() { return repairs.listForCustomer(CurrentUser.get().getId()); }
    @PostMapping("/api/repairs") @ResponseStatus(HttpStatus.CREATED)
    public RepairDto create(@Valid @RequestBody RepairCreateRequest r) { return repairs.create(CurrentUser.get(), r); }
    @GetMapping("/api/repairs/{id}") public RepairDto get(@PathVariable Long id) { return repairs.getForCustomer(CurrentUser.get().getId(), id); }
    @PostMapping("/api/repairs/{id}/quotes/{quoteId}/accept")
    public RepairDto accept(@PathVariable Long id, @PathVariable Long quoteId) { return repairs.acceptQuote(CurrentUser.get(), id, quoteId); }
    @PostMapping("/api/repairs/{id}/quotes/{quoteId}/reject")
    public RepairDto reject(@PathVariable Long id, @PathVariable Long quoteId) { return repairs.rejectQuote(CurrentUser.get(), id, quoteId); }
    @PostMapping("/api/repairs/{id}/cancel") public RepairDto cancel(@PathVariable Long id) { return repairs.cancel(CurrentUser.get(), id); }

    // ---- partner (path secured to ROLE_REPAIR_PARTNER; assignment re-checked in the service) ----
    @GetMapping("/api/partner/repairs") public List<RepairDto> assigned() { return repairs.listForPartner(CurrentUser.get()); }
    @GetMapping("/api/partner/repairs/{id}") public RepairDto partnerGet(@PathVariable Long id) { return repairs.getForPartner(CurrentUser.get(), id); }
    @PostMapping("/api/partner/repairs/{id}/review")
    public RepairDto review(@PathVariable Long id, @Valid @RequestBody(required = false) NoteRequest n) { return repairs.review(CurrentUser.get(), id, n == null ? null : n.note()); }
    @PostMapping("/api/partner/repairs/{id}/quote")
    public RepairDto quote(@PathVariable Long id, @Valid @RequestBody QuoteRequest q) { return repairs.quote(CurrentUser.get(), id, q); }
    @PostMapping("/api/partner/repairs/{id}/reject")
    public RepairDto partnerReject(@PathVariable Long id, @Valid @RequestBody(required = false) NoteRequest n) { return repairs.reject(CurrentUser.get(), id, n == null ? null : n.note()); }
    @PostMapping("/api/partner/repairs/{id}/start") public RepairDto start(@PathVariable Long id) { return repairs.start(CurrentUser.get(), id); }
    @PostMapping("/api/partner/repairs/{id}/complete")
    public RepairDto complete(@PathVariable Long id, @Valid @RequestBody CompleteRepairRequest r) { return repairs.complete(CurrentUser.get(), id, r); }
}
