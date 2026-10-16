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
@Table(name = "repair_status_history")
@Getter
@Setter
@NoArgsConstructor
public class RepairStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "repair_request_id")
    private RepairRequest repairRequest;
    @Enumerated(EnumType.STRING)
    private RepairStatus fromStatus;
    @Enumerated(EnumType.STRING)
    private RepairStatus toStatus;
    private Long actorId;
    private String note;
    private Instant createdAt = Instant.now();
}
