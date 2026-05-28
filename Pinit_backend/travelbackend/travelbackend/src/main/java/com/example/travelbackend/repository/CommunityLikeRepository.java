package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.entity.CommunityLike;
import com.example.travelbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityLikeRepository extends JpaRepository<CommunityLike, Integer> {
    List<CommunityLike> findByUser(User user);
    Optional<CommunityLike> findByUserAndCommunity(User user, Community community);
    void deleteByUserAndCommunity(User user, Community community);
}
