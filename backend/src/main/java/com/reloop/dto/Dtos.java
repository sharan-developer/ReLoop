package com.reloop.dto;

import com.reloop.entity.Enums.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

/** All request/response DTOs. Entities are never serialised directly. */
public final class Dtos {
    private Dtos() {}

    public record PageDto<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
        public static <T> PageDto<T> of(Page<T> p) {
            return new PageDto<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    // ---------- auth / users ----------
    public record RegisterRequest(@NotBlank @Size(max = 120) String fullName,
                                  @NotBlank @Email @Size(max = 160) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password,
                                  @Size(max = 20) String phone) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UserDto(Long id, String email, String fullName, String phone, String role) {}
    public record AuthResponse(String accessToken, UserDto user) {}
    public record UpdateProfileRequest(@NotBlank @Size(max = 120) String fullName, @Size(max = 20) String phone) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record AddressRequest(@NotBlank @Size(max = 40) String label, @NotBlank @Size(max = 160) String line1,
                                 @Size(max = 160) String line2, @NotBlank @Size(max = 80) String city,
                                 @NotBlank @Size(max = 80) String state,
                                 @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit PIN code") String postalCode,
                                 @NotBlank @Size(max = 20) String phone, boolean defaultAddress) {}
    public record AddressDto(Long id, String label, String line1, String line2, String city, String state,
                             String postalCode, String phone, boolean defaultAddress) {}
    public record PartnerApplyRequest(@NotBlank @Size(max = 140) String businessName, @NotBlank @Size(max = 80) String city) {}
    public record PartnerDto(Long id, String businessName, String city) {}

    // ---------- catalog ----------
    public record CategoryDto(Long id, String name, String slug, boolean active) {}
    public record CategoryRequest(@NotBlank @Size(max = 80) String name, Boolean active) {}
    public record ProductDto(Long id, String name, String brand, String description, String condition,
                             BigDecimal price, BigDecimal originalPrice, Integer discountPercent, String imageUrl,
                             List<String> images, Long categoryId, String categoryName, int stock, boolean available,
                             boolean active, Double rating, long reviewCount, int soldCount) {}
    public record ProductRequest(@NotBlank @Size(max = 160) String name, @Size(max = 80) String brand,
                                 @Size(max = 5000) String description, @NotNull ItemCondition condition,
                                 @NotNull @DecimalMin("0.01") BigDecimal price, @DecimalMin("0.01") BigDecimal originalPrice,
                                 @Size(max = 500) String imageUrl, @NotNull Long categoryId,
                                 @NotNull @Min(0) Integer stock, Boolean active) {}
    public record ReviewDto(Long id, String userName, int rating, String comment, Instant createdAt) {}
    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 2000) String comment, @NotNull Long orderItemId) {}

    // ---------- cart / wishlist ----------
    public record CartItemDto(Long id, Long productId, String name, String imageUrl, BigDecimal unitPrice, int quantity,
                              int stock, boolean available, BigDecimal lineTotal) {}
    public record CartDto(List<CartItemDto> items, int itemCount, BigDecimal subtotal, boolean hasUnavailable) {}
    public record CartAddRequest(@NotNull Long productId, @Min(1) @Max(20) int quantity) {}
    public record CartQtyRequest(@Min(1) @Max(20) int quantity) {}

    // ---------- orders ----------
    public record CheckoutRequest(@NotNull Long addressId, @NotNull PaymentMethod paymentMethod) {}
    public record OrderItemDto(Long id, Long productId, Long listingId, String name, BigDecimal unitPrice, int quantity,
                               BigDecimal lineTotal, boolean canReview) {}
    public record OrderDto(Long id, String orderNumber, String status, String paymentMethod, String paymentStatus,
                           String type, BigDecimal subtotal, BigDecimal total, Instant createdAt, String shipName,
                           String shipAddress, List<OrderItemDto> items, String razorpayOrderId, String razorpayKeyId,
                           boolean canCancel) {}
    public record RazorpayVerifyRequest(@NotNull Long orderId, @NotBlank String razorpayOrderId,
                                        @NotBlank String razorpayPaymentId, @NotBlank String razorpaySignature) {}
    public record OrderStatusRequest(@NotNull OrderStatus status) {}
    public record PaymentConfigDto(boolean razorpayEnabled, String keyId, boolean cloudinaryEnabled) {}

    // ---------- lifecycle ----------
    public record OwnedProductRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 80) String category,
                                      @Size(max = 80) String brand, @Size(max = 80) String model,
                                      @Size(max = 80) String serialNumber, @NotNull @PastOrPresent LocalDate purchaseDate,
                                      LocalDate warrantyStart, LocalDate warrantyEnd, @Size(max = 120) String warrantyProvider) {}
    public record OwnedProductDto(Long id, String name, String category, String brand, String model, String serialNumber,
                                  LocalDate purchaseDate, String status, String warrantyStatus, LocalDate warrantyEnd,
                                  boolean hasReceipt, String publicToken, long openRepairs) {}
    public record WarrantyRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate, @Size(max = 120) String provider,
                                  @Size(max = 500) String notes) {}
    public record LifecycleEventDto(Long id, String type, String verification, String actorLabel, String details, Instant createdAt) {}
    public record PassportDto(OwnedProductDto product, List<LifecycleEventDto> events) {}
    public record PublicPassportDto(String name, String category, String brand, String model, String status,
                                    String warrantyStatus, List<LifecycleEventDto> events, String disclaimer) {}
    public record RecycleRequest(@NotNull @PastOrPresent LocalDate recycledOn, @Size(max = 140) String partnerName,
                                 @Size(max = 500) String evidenceUrl) {}

    // ---------- repairs ----------
    public record RepairCreateRequest(@NotNull Long ownedProductId, @NotNull Long partnerId,
                                      @NotBlank @Size(max = 2000) String problem, @Size(max = 5) List<String> imageUrls) {}
    public record QuoteRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @Min(1) @Max(365) int estimatedDays,
                               @Size(max = 1000) String notes) {}
    public record CompleteRepairRequest(@NotBlank @Size(max = 2000) String workNotes, @NotNull @DecimalMin("0") BigDecimal finalCost) {}
    public record NoteRequest(@Size(max = 500) String note) {}
    public record QuoteDto(Long id, BigDecimal amount, int estimatedDays, String notes, String status, Instant createdAt) {}
    public record RepairHistoryDto(String from, String to, String note, Instant createdAt) {}
    public record RepairDto(Long id, Long ownedProductId, String productName, String customerName, String partnerName,
                            String problem, String status, List<String> imageUrls, List<QuoteDto> quotes,
                            List<RepairHistoryDto> history, BigDecimal finalCost, String workNotes, Instant createdAt) {}

    // ---------- resale ----------
    public record ListingRequest(@NotNull Long ownedProductId, @NotBlank @Size(max = 160) String title,
                                 @Size(max = 3000) String description, @NotNull ItemCondition condition,
                                 @NotNull @DecimalMin("1") BigDecimal price, @Size(max = 500) String imageUrl) {}
    public record ListingUpdateRequest(@NotBlank @Size(max = 160) String title, @Size(max = 3000) String description,
                                       @NotNull ItemCondition condition, @NotNull @DecimalMin("1") BigDecimal price,
                                       @Size(max = 500) String imageUrl) {}
    public record ListingDto(Long id, String title, String description, String condition, BigDecimal price, String imageUrl,
                             String status, String sellerName, String productName, String category, String brand,
                             String warrantyStatus, List<LifecycleEventDto> history, Long ownedProductId, boolean mine) {}
    public record SaleDto(Long orderId, String orderNumber, Long listingId, String title, String buyerFirstName, BigDecimal price,
                           String paymentMethod, String status) {}
    public record ReportRequest(@NotBlank @Size(max = 500) String reason) {}

    // ---------- admin ----------
    public record TrendPoint(String month, long count, BigDecimal amount) {}
    public record ActivityDto(String action, String entityType, Long entityId, String details, Instant createdAt) {}
    public record AdminStats(long users, long products, long activeListings, long orders, long repairRequests,
                             long completedRepairs, long pendingApprovals, long reportedListings, BigDecimal totalSales,
                             List<ActivityDto> recent, List<TrendPoint> salesTrend, List<TrendPoint> repairTrend,
                             List<TrendPoint> resaleTrend) {}
    public record AdminUserDto(Long id, String email, String fullName, String role, boolean enabled, Instant createdAt) {}
    public record AdminUserUpdate(Boolean enabled) {}
    public record AdminOrderDto(Long id, String orderNumber, String userEmail, String status, String paymentStatus,
                                BigDecimal total, Instant createdAt) {}
    public record AdminPartnerDto(Long id, Long userId, String email, String businessName, String city, boolean approved) {}
    public record ReportDto(Long id, Long listingId, String listingTitle, String reason, String status, Instant createdAt) {}
    public record AdminReviewDto(Long id, String productName, String userName, int rating, String comment, boolean hidden) {}
    public record AdminEventDto(Long id, Long ownedProductId, String productName, String type, String verification,
                                String actorLabel, String details, Instant createdAt) {}
    public record AdminListingDto(Long id, String title, String sellerEmail, BigDecimal price, String status, Instant createdAt) {}
    public record ListingStatusRequest(@NotNull ListingStatus status) {}
}
