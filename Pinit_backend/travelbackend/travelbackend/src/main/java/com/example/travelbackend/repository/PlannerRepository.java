package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Planner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlannerRepository extends JpaRepository<Planner, Integer> {
    List<Planner> findByUserUserNumber(Integer userNumber);
}