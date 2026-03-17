package com.sentinel.cloud_optimizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.cloud_optimizer.dto.AwsCostRequestDTO;
import com.sentinel.cloud_optimizer.repository.AwsCostRepository;
import com.sentinel.cloud_optimizer.repository.BudgetAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração — fluxo completo com banco H2 em memória.
 *
 * @SpringBootTest: sobe o contexto completo do Spring (todos os beans reais).
 * @AutoConfigureMockMvc: injeta MockMvc sem abrir porta de rede.
 * @ActiveProfiles("test"): ativa application-test.properties com H2.
 *
 * Diferença dos unitários: aqui testamos o comportamento do sistema como um todo,
 * incluindo persistência real, validações em cascata e criação de alertas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AwsCostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AwsCostRepository costRepository;

    @Autowired
    private BudgetAlertRepository alertRepository;

    @BeforeEach
    void limparBanco() {
        // Garante isolamento entre testes: cada teste começa com banco vazio
        alertRepository.deleteAll();
        costRepository.deleteAll();
    }

    @Test
    void devePersistitCustoERetornarComId() throws Exception {
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("EC2-integration");
        request.setCostAmount(15.0);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.resourceName").value("EC2-integration"))
                .andExpect(jsonPath("$.captureDate").isNotEmpty());

        assertThat(costRepository.findAll()).hasSize(1);
    }

    @Test
    void deveCriarAlertaAutomaticamenteQuandoCustoExcedeLimite() throws Exception {
        // Limite configurado em application-test.properties: 40.0
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("RDS-caro");
        request.setCostAmount(99.99);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertThat(costRepository.findAll()).hasSize(1);
        assertThat(alertRepository.findAll()).hasSize(1);
        assertThat(alertRepository.findAll().get(0).getExceededAmount()).isEqualTo(99.99 - 40.0);
    }

    @Test
    void naoDeveCriarAlertaQuandoCustoEstaDentroDoLimite() throws Exception {
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("CloudWatch-barato");
        request.setCostAmount(10.0);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        assertThat(alertRepository.findAll()).isEmpty();
    }

    @Test
    void deveListarCustosComPaginacao() throws Exception {
        // Cria 2 registros
        for (String resource : List.of("EC2", "RDS")) {
            AwsCostRequestDTO req = new AwsCostRequestDTO();
            req.setResourceName(resource);
            req.setCostAmount(20.0);
            mockMvc.perform(post("/api/costs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        mockMvc.perform(get("/api/costs?size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void deveExcluirCustoERemoverDoBanco() throws Exception {
        // Cria um custo
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("S3-temp");
        request.setCostAmount(5.0);

        String responseBody = mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();

        // Deleta e verifica
        mockMvc.perform(delete("/api/costs/" + id))
                .andExpect(status().isNoContent());

        assertThat(costRepository.findAll()).isEmpty();
    }

    @Test
    void deveRetornar400ParaBodyInvalido() throws Exception {
        // resourceName em branco deve ser rejeitado antes de chegar ao banco
        AwsCostRequestDTO request = new AwsCostRequestDTO();
        request.setResourceName("");
        request.setCostAmount(10.0);

        mockMvc.perform(post("/api/costs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(costRepository.findAll()).isEmpty();
    }
}
