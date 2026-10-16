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
@Table(name = "partner_profiles")
@Getter
@Setter
@NoArgsConstructor
public class PartnerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "user_id")
    private User user;
    private String businessName;
    private String city;
    private boolean approved = false;
    private Instant createdAt = Instant.now();
}
