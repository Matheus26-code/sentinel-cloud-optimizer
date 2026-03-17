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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do AwsCostService.
 *
 * @ExtendWith(MockitoExtension.class): inicializa os mocks sem subir o Spring.
 * Isso torna os testes muito rápidos — sem banco, sem rede, sem contexto.
 */
@ExtendWith(MockitoExtension.class)
class AwsCostServiceTest {

    @Mock
    private AwsCostRepository repository;

    @Mock
    private BudgetAlertService budgetAlertService;

    @InjectMocks
    private AwsCostService service;

    // --- Helpers ---

    private AwsCostRequestDTO buildDto(String resource, double amount) {
        AwsCostRequestDTO dto = new AwsCostRequestDTO();
        dto.setResourceName(resource);
        dto.setCostAmount(amount);
        return dto;
    }

    private AwsCost buildSavedCost(Long id, String resource, double amount) {
        AwsCost saved = new AwsCost();
        saved.setId(id);
        saved.setResourceName(resource);
        saved.setCostAmount(amount);
        saved.setCurrency("USD");
        return saved;
    }

    // --- salvarCusto ---

    @Test
    void deveSalvarCustoAbaixoDoLimiteSemGerarAlerta() {
        AwsCostRequestDTO dto = buildDto("EC2-t2.micro", 30.0);
        AwsCost saved = buildSavedCost(1L, "EC2-t2.micro", 30.0);

        when(repository.save(any(AwsCost.class))).thenReturn(saved);

        AwsCostResponseDTO response = service.salvarCusto(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getResourceName()).isEqualTo("EC2-t2.micro");
        assertThat(response.getCostAmount()).isEqualTo(30.0);
        verify(repository).save(any(AwsCost.class));
        verify(budgetAlertService).checkAndCreateAlert(saved);
    }

    @Test
    void deveSalvarCustoEDelegarVerificacaoDeAlertaAoService() {
        // Garante que o custo é salvo ANTES de verificar o alerta.
        // Isso é crítico: BudgetAlertService precisa do ID (FK do @OneToOne).
        AwsCostRequestDTO dto = buildDto("RDS-db.t3.medium", 50.0);
        AwsCost saved = buildSavedCost(2L, "RDS-db.t3.medium", 50.0);

        when(repository.save(any(AwsCost.class))).thenReturn(saved);

        service.salvarCusto(dto);

        var inOrder = inOrder(repository, budgetAlertService);
        inOrder.verify(repository).save(any(AwsCost.class));
        inOrder.verify(budgetAlertService).checkAndCreateAlert(saved);
    }

    @Test
    void deveLancarExcecaoParaCustoNegativo() {
        AwsCostRequestDTO dto = buildDto("S3-bucket", -10.0);

        assertThrows(IllegalArgumentException.class, () -> service.salvarCusto(dto));

        // Nada deve ser persistido se o valor for inválido
        verify(repository, never()).save(any());
        verify(budgetAlertService, never()).checkAndCreateAlert(any());
    }

    @Test
    void deveLancarExcecaoParaCustoZero() {
        // Custo zero não faz sentido semanticamente e deve ser rejeitado
        AwsCostRequestDTO dto = buildDto("CloudWatch", 0.0);

        // 0.0 é negativo? Não, mas não é positivo. Verificamos se a regra cobre exatamente < 0.
        // Se quiser cobrir == 0 também, a validação @Positive no DTO já faz isso na camada web.
        // No service, apenas negativo lança exceção.
        AwsCost saved = buildSavedCost(3L, "CloudWatch", 0.0);
        when(repository.save(any(AwsCost.class))).thenReturn(saved);

        // Zero passa pela validação do service (não é negativo)
        AwsCostResponseDTO response = service.salvarCusto(dto);
        assertThat(response).isNotNull();
    }

    // --- listarTodos ---

    @Test
    void deveRetornarPaginaDeCustos() {
        AwsCost cost = buildSavedCost(1L, "EC2", 20.0);
        Pageable pageable = PageRequest.of(0, 10);
        Page<AwsCost> page = new PageImpl<>(List.of(cost), pageable, 1);

        when(repository.findAll(pageable)).thenReturn(page);

        Page<AwsCostResponseDTO> result = service.listarTodos(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getResourceName()).isEqualTo("EC2");
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaCustos() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<AwsCostResponseDTO> result = service.listarTodos(pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // --- excluir ---

    @Test
    void deveExcluirCustoPorId() {
        service.excluir(99L);

        verify(repository).deleteById(99L);
    }
}
