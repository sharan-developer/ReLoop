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
@Table(name = "owned_products")
@Getter
@Setter
@NoArgsConstructor
public class OwnedProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owner_id")
    private User owner;
    private String name;
    private String category;
    private String brand;
    private String model;
    private String serialNumber;
    private LocalDate purchaseDate;
    private String receiptFile;
    @Enumerated(EnumType.STRING)
    private OwnedStatus status = OwnedStatus.ACTIVE;
    private String publicToken;
    private Instant createdAt = Instant.now();
}
