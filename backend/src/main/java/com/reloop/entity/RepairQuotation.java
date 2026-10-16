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
@Table(name = "repair_quotations")
@Getter
@Setter
@NoArgsConstructor
public class RepairQuotation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "repair_request_id")
    private RepairRequest repairRequest;
    private BigDecimal amount;
    private int estimatedDays = 1;
    private String notes;
    @Enumerated(EnumType.STRING)
    private QuoteStatus status = QuoteStatus.PENDING;
    private Instant createdAt = Instant.now();
}
