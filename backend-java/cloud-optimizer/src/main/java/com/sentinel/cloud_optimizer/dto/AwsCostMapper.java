package com.sentinel.cloud_optimizer.dto;

import com.sentinel.cloud_optimizer.model.AwsCost;

/**
 * Mapper manual entre DTO e Entidade.
 *
 * Alternativa: MapStruct gera esse código via annotation processor em compile-time.
 * Para este portfólio, o mapper manual é mais didático e sem dependência extra.
 */
public class AwsCostMapper {

    private AwsCostMapper() {
        // Classe utilitária — não deve ser instanciada
    }

    /** Converte o DTO de entrada em entidade JPA para persistência. */
    public static AwsCost toEntity(AwsCostRequestDTO dto) {
        AwsCost entity = new AwsCost();
        entity.setResourceName(dto.getResourceName());
        entity.setCostAmount(dto.getCostAmount());
        return entity;
    }

    /** Converte a entidade persistida em DTO de resposta para a API. */
    public static AwsCostResponseDTO toResponse(AwsCost entity) {
        AwsCostResponseDTO dto = new AwsCostResponseDTO();
        dto.setId(entity.getId());
        dto.setResourceName(entity.getResourceName());
        dto.setCostAmount(entity.getCostAmount());
        dto.setCurrency(entity.getCurrency());
        dto.setCaptureDate(entity.getCaptureDate());
        return dto;
    }
}
