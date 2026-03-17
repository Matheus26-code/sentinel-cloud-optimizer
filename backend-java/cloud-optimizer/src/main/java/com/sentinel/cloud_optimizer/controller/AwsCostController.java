package com.sentinel.cloud_optimizer.controller;

import com.sentinel.cloud_optimizer.dto.AwsCostRequestDTO;
import com.sentinel.cloud_optimizer.dto.AwsCostResponseDTO;
import com.sentinel.cloud_optimizer.service.AwsCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gerenciamento de custos AWS.
 * CORS configurado globalmente em WebConfig — sem @CrossOrigin aqui.
 */
@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
@Tag(name = "AWS Costs", description = "Gerenciamento de custos de recursos AWS")
public class AwsCostController {

    private final AwsCostService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um novo custo de recurso AWS")
    @ApiResponse(responseCode = "201", description = "Custo criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (recurso em branco, valor negativo, etc.)")
    public AwsCostResponseDTO criar(@Valid @RequestBody AwsCostRequestDTO dto) {
        // @Valid ativa as anotações de validação do DTO (ex: @NotBlank, @Positive)
        return service.salvarCusto(dto);
    }

    @GetMapping
    @Operation(summary = "Lista custos com paginação",
               description = "Aceita parâmetros: ?page=0&size=10&sort=costAmount,desc")
    public Page<AwsCostResponseDTO> listar(Pageable pageable) {
        return service.listarTodos(pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 é mais semântico que 200 para DELETE
    @Operation(summary = "Remove um custo pelo ID")
    @ApiResponse(responseCode = "204", description = "Removido com sucesso")
    public void deletar(@PathVariable Long id) {
        service.excluir(id);
    }
}
