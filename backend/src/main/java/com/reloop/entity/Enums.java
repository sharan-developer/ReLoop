package com.reloop.entity;

public final class Enums {
    private Enums() {}
    public enum Role { CUSTOMER, REPAIR_PARTNER, ADMIN }
    public enum ItemCondition { NEW, REFURBISHED, USED }
    public enum OrderStatus { PENDING_PAYMENT, PLACED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }
    public enum PaymentMethod { COD, RAZORPAY }
    public enum PaymentStatus { PENDING, PAID, FAILED }
    public enum OrderType { STORE, RESALE }
    public enum OwnedStatus { ACTIVE, LISTED, RECYCLED }
    public enum EventType { PRODUCT_REGISTERED, PURCHASE_RECORDED, WARRANTY_UPDATED, REPAIR_REQUESTED, REPAIR_APPROVED, REPAIR_STARTED, REPAIR_COMPLETED, RESALE_LISTED, OWNERSHIP_TRANSFERRED, RECYCLING_RECORDED }
    public enum Verification { SELF_REPORTED, PARTNER_CONFIRMED, ADMIN_CONFIRMED }
    public enum RepairStatus { REQUESTED, REVIEWED, QUOTED, APPROVED, IN_PROGRESS, COMPLETED, REJECTED, CANCELLED }
    public enum QuoteStatus { PENDING, ACCEPTED, REJECTED }
    public enum ListingStatus { DRAFT, PUBLISHED, PAUSED, RESERVED, SOLD }
    public enum ReportStatus { OPEN, RESOLVED }
    public enum RecyclingStatus { SELF_REPORTED, CONFIRMED }
}
