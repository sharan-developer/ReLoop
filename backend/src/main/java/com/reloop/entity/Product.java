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
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "category_id")
    private Category category;
    private String name;
    private String brand;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(name = "condition_type")
    private ItemCondition itemCondition;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageUrl;
    private boolean active = true;
    private int soldCount = 0;
    private Instant createdAt = Instant.now();
}
