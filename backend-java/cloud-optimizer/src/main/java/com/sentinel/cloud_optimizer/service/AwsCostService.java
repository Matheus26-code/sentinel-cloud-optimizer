package com.sentinel.cloud_optimizer.service;

import com.sentinel.cloud_optimizer.dto.AwsCostMapper;
import com.sentinel.cloud_optimizer.dto.AwsCostRequestDTO;
import com.sentinel.cloud_optimizer.dto.AwsCostResponseDTO;
import com.sentinel.cloud_optimizer.model.AwsCost;
import com.sentinel.cloud_optimizer.repository.AwsCostRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Orquestra as operações de custos AWS.
 * Lógica de alertas delegada ao BudgetAlertService (SRP).
 */
@Service
@RequiredArgsConstructor
public class AwsCostService {

    private static final Logger log = LoggerFactory.getLogger(AwsCostService.class);

    // 'final' + @RequiredArgsConstructor = constructor injection automático.
    // Benefícios sobre @Autowired em campo:
    // 1. Imutabilidade: o campo não pode ser reatribuído
    // 2. Testabilidade: facilita instanciar a classe manualmente nos testes
    // 3. Falha rápida: dependência nula falha no startup, não em runtime
    private final AwsCostRepository repository;
    private final BudgetAlertService budgetAlertService;

    /**
     * Persiste um novo custo e dispara alerta se necessário.
     *
     * Ordem corrigida em relação à versão anterior:
     * 1. Valida o DTO
     * 2. Converte para entidade
     * 3. Salva o custo (gera o ID)
     * 4. Verifica alerta com o custo já persistido (ID disponível para o @OneToOne)
     */
    public AwsCostResponseDTO salvarCusto(AwsCostRequestDTO dto) {
        if (dto.getCostAmount() < 0) {
            throw new IllegalArgumentException("O valor do custo não pode ser negativo.");
        }

        AwsCost entity = AwsCostMapper.toEntity(dto);
        AwsCost saved = repository.save(entity);

        log.info("Cost saved: id={}, resource='{}', amount={}",
                saved.getId(), saved.getResourceName(), saved.getCostAmount());

        budgetAlertService.checkAndCreateAlert(saved);

        return AwsCostMapper.toResponse(saved);
    }

    /**
     * Lista custos com paginação.
     * Pageable aceita parâmetros ?page=0&size=10&sort=costAmount,desc via query string.
     */
    public Page<AwsCostResponseDTO> listarTodos(Pageable pageable) {
        return repository.findAll(pageable).map(AwsCostMapper::toResponse);
    }

    public void excluir(Long id) {
        repository.deleteById(id);
        log.info("Cost deleted: id={}", id);
    }
}
