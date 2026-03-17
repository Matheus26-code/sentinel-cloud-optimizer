package com.sentinel.cloud_optimizer.service;

import com.sentinel.cloud_optimizer.model.AwsCost;
import com.sentinel.cloud_optimizer.model.BudgetsAlerts;
import com.sentinel.cloud_optimizer.repository.BudgetAlertRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Responsável exclusivamente pela lógica de alertas de orçamento.
 *
 * SRP (Single Responsibility Principle): separamos essa responsabilidade
 * do AwsCostService para que cada classe tenha apenas um motivo para mudar.
 * Se a lógica de alertas evoluir (ex: enviar email, notificar Slack),
 * apenas esta classe precisa ser modificada.
 */
@Service
@RequiredArgsConstructor // Lombok gera o construtor com os campos 'final' — sem @Autowired
public class BudgetAlertService {

    private static final Logger log = LoggerFactory.getLogger(BudgetAlertService.class);

    private final BudgetAlertRepository alertRepository;

    /**
     * Limite de alerta lido do application.properties.
     * Valor padrão de 40.0 se a propriedade não estiver configurada.
     * Centralizar configuração aqui evita "números mágicos" espalhados no código.
     */
    @Value("${budget.alert.limit:40.0}")
    private Double alertLimit;

    /**
     * Verifica se o custo salvo excede o limite e, em caso positivo, cria um alerta.
     *
     * @param savedCost Custo já persistido no banco (com ID gerado).
     *                  Recebemos após o save() do AwsCostService para garantir
     *                  que o @OneToOne do alerta aponte para um registro existente.
     */
    public void checkAndCreateAlert(AwsCost savedCost) {
        if (savedCost.getCostAmount() > alertLimit) {
            BudgetsAlerts alert = new BudgetsAlerts();
            alert.setMessage("Custo excessivo detectado para: " + savedCost.getResourceName());
            alert.setExceededAmount(savedCost.getCostAmount() - alertLimit);
            alert.setOriginCost(savedCost); // referência segura — savedCost já tem ID

            alertRepository.save(alert);

            log.warn("Budget alert created: resource='{}', amount={}, exceeded={}",
                    savedCost.getResourceName(),
                    savedCost.getCostAmount(),
                    savedCost.getCostAmount() - alertLimit);
        }
    }
}
