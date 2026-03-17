package com.sentinel.cloud_optimizer.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_alerts")
@Data
public class BudgetsAlerts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private Double exceededAmount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime alertDate;

    @OneToOne
    @JoinColumn(name = "cost_id")
    private AwsCost originCost;
}
