package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Planner;
import com.example.travelbackend.repository.PlannerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/planner")
@CrossOrigin(origins = "*")
public class PlannerController {

    @Autowired
    private PlannerRepository plannerRepository;

    // 전체 플래너 조회
    @GetMapping
    public List<Planner> getAllPlanners() {
        return plannerRepository.findAll();
    }

    // 유저별 플래너 조회
    @GetMapping("/user/{userNumber}")
    public List<Planner> getPlannersByUser(@PathVariable Integer userNumber) {
        return plannerRepository.findByUserUserNumber(userNumber);
    }

    // 플래너 추가
    @PostMapping
    public Planner addPlanner(@RequestBody Planner planner) {
        return plannerRepository.save(planner);
    }

    // 플래너 삭제
    @DeleteMapping("/{id}")
    public void deletePlanner(@PathVariable Integer id) {
        plannerRepository.deleteById(id);
    }
}