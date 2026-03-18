package com.sentinel.cloud_optimizer.service;

import com.sentinel.cloud_optimizer.model.AwsCost;
import com.sentinel.cloud_optimizer.model.BudgetsAlerts;
import com.sentinel.cloud_optimizer.repository.AwsCostRepository;
import com.sentinel.cloud_optimizer.repository.BudgetAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AwsCostService {

    @Autowired
    private AwsCostRepository repository;

    @Autowired
    private BudgetAlertRepository alertRepository;

    private static final Double LIMITE_ALERTA = 40.0;

    public AwsCost salvarCusto(AwsCost custo) {
        if (custo.getCostAmount() > LIMITE_ALERTA) {
            BudgetsAlerts alerta = new BudgetsAlerts();
            alerta.setMessage("Custo excessivo detectado!");
            alerta.setExceededAmount(custo.getCostAmount() - LIMITE_ALERTA);
            alerta.setOriginCost(custo);

            alertRepository.save(alerta);
            System.out.println("⚠️ ALERTA: Gasto excedido para " + custo.getResourceName());
        }
        return repository.save(custo);
    }

    public List<AwsCost> listarTodos() {
        return repository.findAll();
    }

    public void excluir(Long id) {
        repository.deleteById(id);
    }
}