package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {
    List<Plan> findByPlannerPlannerNumber(Integer plannerNumber);
}