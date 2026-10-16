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
@Table(name = "recycling_records")
@Getter
@Setter
@NoArgsConstructor
public class RecyclingRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owned_product_id")
    private OwnedProduct ownedProduct;
    private LocalDate recycledOn;
    private String partnerName;
    private String evidenceUrl;
    @Enumerated(EnumType.STRING)
    private RecyclingStatus status = RecyclingStatus.SELF_REPORTED;
    private Long recordedBy;
}
