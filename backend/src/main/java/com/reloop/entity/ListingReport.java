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
@Table(name = "listing_reports")
@Getter
@Setter
@NoArgsConstructor
public class ListingReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "listing_id")
    private ResaleListing listing;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "reporter_id")
    private User reporter;
    private String reason;
    @Enumerated(EnumType.STRING)
    private ReportStatus status = ReportStatus.OPEN;
    private Instant createdAt = Instant.now();
}
