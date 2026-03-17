package com.sentinel.cloud_optimizer;

import com.sentinel.cloud_optimizer.dto.AwsCostRequestDTO;
import com.sentinel.cloud_optimizer.dto.AwsCostResponseDTO;
import com.sentinel.cloud_optimizer.model.AwsCost;
import com.sentinel.cloud_optimizer.repository.AwsCostRepository;
import com.sentinel.cloud_optimizer.service.AwsCostService;
import com.sentinel.cloud_optimizer.service.BudgetAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsCostServiceTest {

    @Mock
    private AwsCostRepository repository;

    // BudgetAlertService agora é quem encapsula a lógica de alertas
    @Mock
    private BudgetAlertService budgetAlertService;

    @InjectMocks
    private AwsCostService service;

    // --- Helpers para não repetir código de setup nos testes ---

    private AwsCostRequestDTO buildDto(String resource, double amount) {
        AwsCostRequestDTO dto = new AwsCostRequestDTO();
        dto.setResourceName(resource);
        dto.setCostAmount(amount);
        return dto;
    }

    private AwsCost buildSavedCost(String resource, double amount) {
        AwsCost saved = new AwsCost();
        saved.setId(1L);
        saved.setResourceName(resource);
        saved.setCostAmount(amount);
        saved.setCurrency("USD");
        return saved;
    }

    // ---------------------------------------------------------------

    @Test
    void deveSalvarCustoSemGerarAlerta() {
        // DADO um custo abaixo do limite
        AwsCostRequestDTO dto = buildDto("EC2-t2.micro", 30.0);
        AwsCost saved = buildSavedCost("EC2-t2.micro", 30.0);

        when(repository.save(any(AwsCost.class))).thenReturn(saved);

        // QUANDO salvarCusto é chamado
        AwsCostResponseDTO response = service.salvarCusto(dto);

        // ENTÃO o custo é salvo e nenhum alerta é criado
        assertThat(response).isNotNull();
        assertThat(response.getResourceName()).isEqualTo("EC2-t2.micro");
        verify(repository).save(any(AwsCost.class));
        // BudgetAlertService é chamado, mas internamente decide não criar alerta
        verify(budgetAlertService).checkAndCreateAlert(saved);
    }

    @Test
    void deveSalvarCustoEDelegarVerificacaoDeAlerta() {
        // DADO um custo acima do limite (40.0)
        AwsCostRequestDTO dto = buildDto("RDS-db.t3.medium", 50.0);
        AwsCost saved = buildSavedCost("RDS-db.t3.medium", 50.0);

        when(repository.save(any(AwsCost.class))).thenReturn(saved);

        // QUANDO salvarCusto é chamado
        service.salvarCusto(dto);

        // ENTÃO o custo é salvo ANTES de verificar alerta (ordem importa para o @OneToOne)
        var inOrder = inOrder(repository, budgetAlertService);
        inOrder.verify(repository).save(any(AwsCost.class));
        inOrder.verify(budgetAlertService).checkAndCreateAlert(saved);
    }

    @Test
    void deveLancarExcecaoParaCustoNegativo() {
        // DADO um custo com valor negativo
        AwsCostRequestDTO dto = buildDto("S3-bucket", -10.0);

        // QUANDO e ENTÃO: IllegalArgumentException deve ser lançada
        assertThrows(IllegalArgumentException.class, () -> service.salvarCusto(dto));

        // E nenhuma persistência deve acontecer
        verify(repository, never()).save(any());
        verify(budgetAlertService, never()).checkAndCreateAlert(any());
    }
}
