package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Budget;
import com.example.travelbackend.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/budget")
@CrossOrigin(origins = "*")
public class BudgetController {

    @Autowired
    private BudgetRepository budgetRepository;

    // 플래너별 예산 조회
    @GetMapping("/planner/{plannerNumber}")
    public List<Budget> getBudgetsByPlanner(@PathVariable Integer plannerNumber) {
        return budgetRepository.findByPlannerPlannerNumber(plannerNumber);
    }

    // 예산 추가
    @PostMapping
    public Budget addBudget(@RequestBody Budget budget) {
        return budgetRepository.save(budget);
    }

    // 예산 삭제
    @DeleteMapping("/{id}")
    public void deleteBudget(@PathVariable Integer id) {
        budgetRepository.deleteById(id);
    }
}