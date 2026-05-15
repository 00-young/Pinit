package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommunityRepository extends JpaRepository<Community, Integer> {
    List<Community> findByUser(User user);
    List<Community> findByCommunityCategory(String category);
}
