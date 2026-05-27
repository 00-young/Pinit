package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.entity.Scrap;
import com.example.travelbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapRepository extends JpaRepository<Scrap, Integer> {
    List<Scrap> findByUser(User user);
    Optional<Scrap> findByUserAndCommunity(User user, Community community);
    void deleteByUserAndCommunity(User user, Community community);
}
