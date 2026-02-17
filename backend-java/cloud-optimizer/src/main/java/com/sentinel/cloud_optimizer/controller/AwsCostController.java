package com.sentinel.cloud_optimizer.controller;

import com.sentinel.cloud_optimizer.model.AwsCost;
import com.sentinel.cloud_optimizer.service.AwsCostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/costs")
@CrossOrigin(origins = "*")
public class AwsCostController {

    @Autowired
    private AwsCostService service;

    @PostMapping
    public AwsCost criar(@RequestBody AwsCost custo) {
        return service.salvarCusto(custo);
    }

    @GetMapping
    public List<AwsCost> listar() {
        return service.listarTodos();
    }
}