package com.industry.simulator.assembly.controller;

import com.industry.simulator.assembly.entity.ProductionRule;
import com.industry.simulator.assembly.entity.ProductionRuleInput;
import com.industry.simulator.assembly.repository.ProductionRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Portal de Configuracoes - Mapeamento Generico (Seccao 6.3): regras de
 * transformacao e arvore de componentes (BOM) desta camada.
 */
@RestController
@RequestMapping("/api/assembly/production-rules")
@CrossOrigin("*")
public class ProductionRuleController {

    /** O enunciado exige, no minimo, 2 unidades da camada anterior por unidade produzida. */
    private static final int MIN_INPUT_QUANTITY = 2;

    @Autowired
    private ProductionRuleRepository repository;

    @GetMapping
    public ResponseEntity<List<ProductionRule>> list() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody ProductionRule rule) {
        String error = validate(rule);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        rule.setId(null);
        rule.getInputs().forEach(i -> { i.setId(null); i.setRule(rule); });
        return ResponseEntity.ok(repository.save(rule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable Long id, @RequestBody ProductionRule rule) {
        String error = validate(rule);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("error", error));
        }
        rule.setId(id);
        rule.getInputs().forEach(i -> i.setRule(rule));
        return ResponseEntity.ok(repository.save(rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Rejeita regras que violem a regra de consumo da cadeia. */
    private String validate(ProductionRule rule) {
        if (rule.getOutputMaterial() == null || rule.getOutputMaterial().isBlank()) {
            return "O material produzido e obrigatorio.";
        }
        if (rule.getInputs() == null || rule.getInputs().isEmpty()) {
            return "A regra tem de consumir pelo menos um insumo.";
        }
        int total = 0;
        for (ProductionRuleInput input : rule.getInputs()) {
            if (input.getInputMaterial() == null || input.getInputMaterial().isBlank()) {
                return "Cada insumo tem de indicar o material.";
            }
            if (input.getInputQuantity() < 1) {
                return "A quantidade de cada insumo tem de ser pelo menos 1.";
            }
            total += input.getInputQuantity();
        }
        if (total < MIN_INPUT_QUANTITY) {
            return "A regra tem de consumir, no minimo, " + MIN_INPUT_QUANTITY
                    + " unidades da camada anterior (regra de consumo da cadeia).";
        }
        return null;
    }
}
