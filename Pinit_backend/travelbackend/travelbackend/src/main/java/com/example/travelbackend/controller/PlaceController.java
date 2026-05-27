package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Place;
import com.example.travelbackend.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/places")
@CrossOrigin(origins = "*")
public class PlaceController {

    @Autowired
    private PlaceRepository placeRepository;

    // 전체 장소 조회
    @GetMapping
    public List<Place> getAllPlaces() {
        return placeRepository.findAll();
    }

    // 장소 이름으로 검색
    @GetMapping("/search")
    public List<Place> searchPlaces(@RequestParam String query) {
        return placeRepository.findByPlaceNameContaining(query);
    }

    // 카테고리로 검색
    @GetMapping("/category")
    public List<Place> getByCategory(@RequestParam String category) {
        return placeRepository.findByPlaceCategory(category);
    }

    // 장소 추가
    @PostMapping
    public Place addPlace(@RequestBody Place place) {
        return placeRepository.save(place);
    }

    // 장소 삭제
    @DeleteMapping("/{id}")
    public void deletePlace(@PathVariable Integer id) {
        placeRepository.deleteById(id);
    }
}