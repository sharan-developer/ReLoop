package com.reloop.entity;

import com.reloop.entity.Enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repair_requests")
@Getter
@Setter
@NoArgsConstructor
public class RepairRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owned_product_id")
    private OwnedProduct ownedProduct;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "customer_id")
    private User customer;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "partner_id")
    private PartnerProfile partner;
    private String problem;
    @Enumerated(EnumType.STRING)
    private RepairStatus status = RepairStatus.REQUESTED;
    private BigDecimal finalCost;
    private String workNotes;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
