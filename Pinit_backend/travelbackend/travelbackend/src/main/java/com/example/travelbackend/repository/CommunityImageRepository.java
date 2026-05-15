package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.entity.CommunityImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommunityImageRepository extends JpaRepository<CommunityImage, Integer> {
    List<CommunityImage> findByCommunity(Community community);
}
