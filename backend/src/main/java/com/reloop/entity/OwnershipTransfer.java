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
@Table(name = "ownership_transfers")
@Getter
@Setter
@NoArgsConstructor
public class OwnershipTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owned_product_id")
    private OwnedProduct ownedProduct;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "from_user_id")
    private User fromUser;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "to_user_id")
    private User toUser;
    private Long listingId;
    private Long orderId;
    private Instant createdAt = Instant.now();
}
