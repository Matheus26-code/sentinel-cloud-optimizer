package com.sentinel.cloud_optimizer.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "aws_costs")
@Data // O Lombok cria os getters e setters automaticamente
public class AwsCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String resourceName; // Nome do recurso (ex: Instância EC2)
    private Double costAmount;   // Valor em dólares
    private String currency = "USD";
    private LocalDateTime captureDate = LocalDateTime.now();
}