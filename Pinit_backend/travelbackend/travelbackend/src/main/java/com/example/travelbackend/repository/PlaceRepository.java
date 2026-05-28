package com.example.travelbackend.repository;

import com.example.travelbackend.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Integer> {
    List<Place> findByPlaceNameContaining(String name);
    List<Place> findByPlaceCategory(String category);
    List<Place> findByPlaceAddressContaining(String address);
}