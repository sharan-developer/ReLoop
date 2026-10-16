package com.reloop.controller;

import com.reloop.dto.Dtos.*;
import com.reloop.entity.User;
import com.reloop.security.CurrentUser;
import com.reloop.service.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class LifecycleController {
    private final OwnedProductService owned;
    private final StorageService storage;

    @GetMapping("/api/my-products") public List<OwnedProductDto> list() { return owned.list(CurrentUser.get().getId()); }

    @PostMapping("/api/my-products") @ResponseStatus(HttpStatus.CREATED)
    public OwnedProductDto register(@Valid @RequestBody OwnedProductRequest r) { return owned.register(CurrentUser.get(), r); }

    @GetMapping("/api/my-products/{id}") public OwnedProductDto get(@PathVariable Long id) { return owned.get(id, CurrentUser.get().getId()); }

    @GetMapping("/api/my-products/{id}/passport") public PassportDto passport(@PathVariable Long id) { return owned.passport(id, CurrentUser.get().getId()); }

    @PostMapping("/api/my-products/{id}/warranty")
    public OwnedProductDto warranty(@PathVariable Long id, @Valid @RequestBody WarrantyRequest r) { return owned.setWarranty(CurrentUser.get(), id, r); }

    @PostMapping("/api/my-products/{id}/recycle")
    public OwnedProductDto recycle(@PathVariable Long id, @Valid @RequestBody RecycleRequest r) { return owned.recycle(CurrentUser.get(), id, r); }

    @PostMapping(value = "/api/my-products/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OwnedProductDto uploadReceipt(@PathVariable Long id, @RequestPart("file") MultipartFile file) { return owned.uploadReceipt(CurrentUser.get(), id, file); }

    /** Private receipt: owner only, never cacheable, never sniffed. */
    @GetMapping("/api/my-products/{id}/receipt")
    public ResponseEntity<Resource> receipt(@PathVariable Long id) {
        User u = CurrentUser.get();
        Resource res = owned.receipt(u, id);
        String name = owned.receiptName(u, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(StorageService.contentTypeFor(name)))
                .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "private, no-store").body(res);
    }

    // ---- public passport (safe fields only) ----
    @GetMapping("/api/passports/public/{token}") public PublicPassportDto publicPassport(@PathVariable String token) { return owned.publicPassport(token); }

    @GetMapping(value = "/api/passports/public/{token}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable String token) { return owned.qrPng(token); }

    // ---- public image upload (any logged-in user) ----
    @PostMapping(value = "/api/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) { return Map.of("url", storage.storePublicImage(file)); }
}
