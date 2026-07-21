package com.kushan.cource_svc.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "cc_payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private String studentId;

    private Integer amountCents = 0;
    private String currency = "USD";

    private String stripeSessionId;
    private String stripePaymentIntentId;

    // CREATED | PAID | FAILED | REFUNDED
    @Column(nullable = false)
    private String status = "CREATED";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
