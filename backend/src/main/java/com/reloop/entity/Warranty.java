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
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
public class Warranty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owned_product_id")
    private OwnedProduct ownedProduct;
    private String provider;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}
