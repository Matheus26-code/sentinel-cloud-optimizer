package com.sentinel.cloud_optimizer.model;

import jakarta.persistence.*;
import lombok.Data;
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
    private LocalDateTime alertDate = LocalDateTime.now();

    // Vamos salvar qual custo gerou esse alerta
    @OneToOne
    @JoinColumn(name = "cost_id")
    private AwsCost originCost;
}