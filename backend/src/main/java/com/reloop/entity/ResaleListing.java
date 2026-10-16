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
@Table(name = "resale_listings")
@Getter
@Setter
@NoArgsConstructor
public class ResaleListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owned_product_id")
    private OwnedProduct ownedProduct;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "seller_id")
    private User seller;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(name = "condition_type")
    private ItemCondition itemCondition;
    private BigDecimal price;
    private String imageUrl;
    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.DRAFT;
    private Instant createdAt = Instant.now();
}
