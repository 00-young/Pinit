package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Place;
import com.example.travelbackend.entity.Review;
import com.example.travelbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByPlace(Place place);
    List<Review> findByUser(User user);
}
