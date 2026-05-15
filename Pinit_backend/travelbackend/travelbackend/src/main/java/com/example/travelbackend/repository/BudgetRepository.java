package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Integer> {
    List<Budget> findByPlannerPlannerNumber(Integer plannerNumber);
}