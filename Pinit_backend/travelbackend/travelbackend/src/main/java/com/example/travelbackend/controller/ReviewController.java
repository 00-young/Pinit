package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Place;
import com.example.travelbackend.entity.Review;
import com.example.travelbackend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @GetMapping("/place/{placeNumber}")
    public List<Review> getReviewsByPlace(@PathVariable Integer placeNumber) {
        Place place = new Place();
        place.setPlaceNumber(placeNumber);
        return reviewRepository.findByPlace(place);
    }

    @PostMapping
    public Review createReview(@RequestBody Review review) {
        if (review.getReviewDate() == null) {
            review.setReviewDate(LocalDate.now());
        }
        return reviewRepository.save(review);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteReview(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        return reviewRepository.findById(id)
                .map(review -> {
                    reviewRepository.delete(review);
                    response.put("success", true);
                    response.put("message", "리뷰가 삭제되었습니다.");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
