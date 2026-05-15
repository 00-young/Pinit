package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Plan;
import com.example.travelbackend.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/plan")
@CrossOrigin(origins = "*")
public class PlanController {

    @Autowired
    private PlanRepository planRepository;

    // 플래너별 일정 조회
    @GetMapping("/planner/{plannerNumber}")
    public List<Plan> getPlansByPlanner(@PathVariable Integer plannerNumber) {
        return planRepository.findByPlannerPlannerNumber(plannerNumber);
    }

    // 일정 추가
    @PostMapping
    public Plan addPlan(@RequestBody Plan plan) {
        return planRepository.save(plan);
    }

    // 일정 삭제
    @DeleteMapping("/{id}")
    public void deletePlan(@PathVariable Integer id) {
        planRepository.deleteById(id);
    }
}