package com.sentinel.cloud_optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.cloud_optimizer.controller.AwsCostController;
import com.sentinel.cloud_optimizer.dto.AwsCostRequestDTO;
import com.sentinel.cloud_optimizer.dto.AwsCostResponseDTO;
import com.sentinel.cloud_optimizer.service.AwsCostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes unitários da camada web (Controller).
 *
 * @WebMvcTest: sobe apenas o contexto MVC (Controllers, Filters, WebConfig).
 * Não sobe o banco, não sobe Services reais — por isso usamos @MockBean.
 *
 * MockMvc: simula requisições HTTP sem abrir porta de rede.
 * Permite testar serialização, validação, status codes e headers.
 */
@WebMvcTest(AwsCostController.class)
class AwsCostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // @MockBean substitui o bean real do Spring context por um mock do Mockito
    @MockBean
    private AwsCostService service;

    // --- Helpers ---

    private AwsCostResponseDTO buildResponse(Long id, String resource, double amount) {
        AwsCostResponseDTO dto = new AwsCostResponseDTO();
        dto.setId(id);
        dto.setResourceName(resource);
        dto.setCostAmount(amount);
        dto.setCurrency("USD");
        dto.setCaptureDate(LocalDateTime.now());
        return dto;
    }

    // --- POST /api/costs ---

    @Test
    void deveRetornar201AoCriarCustoValido() throws Exception {
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("EC2-t2.micro");
        request.setCostAmount(25.0);

        AwsCostResponseDTO response = buildResponse(1L, "EC2-t2.micro", 25.0);
        when(service.salvarCusto(any(AwsCostRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())             // 201
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.resourceName").value("EC2-t2.micro"))
                .andExpect(jsonPath("$.costAmount").value(25.0))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void deveRetornar400QuandoResourceNameEstiverEmBranco() throws Exception {
        // @NotBlank no DTO deve disparar validação antes de chegar ao service
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("");   // inválido
        request.setCostAmount(25.0);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());         // 400

        verify(service, never()).salvarCusto(any());
    }

    @Test
    void deveRetornar400QuandoCostAmountForNulo() throws Exception {
        // @NotNull no DTO deve rejeitar body sem o campo costAmount
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("RDS");
        // costAmount não setado = null

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).salvarCusto(any());
    }

    @Test
    void deveRetornar400QuandoCostAmountForNegativo() throws Exception {
        // @Positive no DTO rejeita valores <= 0
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("S3");
        request.setCostAmount(-5.0);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(service, never()).salvarCusto(any());
    }

    @Test
    void deveRetornar400QuandoBodyEstiverVazio() throws Exception {
        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/costs ---

    @Test
    void deveRetornar200ComPaginaDeCustos() throws Exception {
        AwsCostResponseDTO item = buildResponse(1L, "EC2", 30.0);
        var page = new PageImpl<>(List.of(item));

        when(service.listarTodos(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/costs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].resourceName").value("EC2"))
                .andExpect(jsonPath("$.content[0].costAmount").value(30.0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deveAceitarParametrosDePaginacao() throws Exception {
        when(service.listarTodos(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/costs?page=0&size=5&sort=costAmount,desc"))
                .andExpect(status().isOk());
    }

    // --- DELETE /api/costs/{id} ---

    @Test
    void deveRetornar204AoDeletarCusto() throws Exception {
        doNothing().when(service).excluir(1L);

        mockMvc.perform(delete("/api/costs/1"))
                .andExpect(status().isNoContent()); // 204
    }
}
