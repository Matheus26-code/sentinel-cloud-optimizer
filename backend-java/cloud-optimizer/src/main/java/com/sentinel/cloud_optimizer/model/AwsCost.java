package com.sentinel.cloud_optimizer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "aws_costs")
@Data
public class AwsCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String resourceName;

    @Positive
    private Double costAmount;

    private String currency = "USD";

    /**
     * @CreationTimestamp: o Hibernate define o valor automaticamente no INSERT.
     * É mais confiável que LocalDateTime.now() no campo, que é avaliado
     * quando o objeto Java é criado — não quando é persistido no banco.
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime captureDate;
}
