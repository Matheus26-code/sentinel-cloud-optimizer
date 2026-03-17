package com.sentinel.cloud_optimizer.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO de resposta para os endpoints GET e POST.
 *
 * Por que não retornar a entidade JPA diretamente?
 * 1. Mass assignment: o cliente poderia tentar enviar campos internos (id, captureDate)
 * 2. Acoplamento: qualquer mudança na tabela do banco mudaria a API pública
 * 3. Serialização circular: relacionamentos @OneToOne/@ManyToOne podem causar loop infinito
 */
@Data
public class AwsCostResponseDTO {

    private Long id;
    private String resourceName;
    private Double costAmount;
    private String currency;
    private LocalDateTime captureDate;
}
