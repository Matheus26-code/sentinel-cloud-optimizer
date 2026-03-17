package com.sentinel.cloud_optimizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AwsCostRequestDTO {

    @NotBlank
    private String resourceName;

    @NotNull
    @Positive
    private Double costAmount;
}
