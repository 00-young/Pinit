package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Follow;
import com.example.travelbackend.entity.User;
import com.example.travelbackend.repository.FollowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/follow")
@CrossOrigin(origins = "*")
public class FollowController {

    @Autowired
    private FollowRepository followRepository;

    @PostMapping("/{followerId}/{followingId}")
    public ResponseEntity<Map<String, Object>> follow(@PathVariable Integer followerId, @PathVariable Integer followingId) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Follow> existing = followRepository.findByFollowerAndFollowing(
                userWithId(followerId), userWithId(followingId));
        
        if (existing.isPresent()) {
            response.put("success", false);
            response.put("message", "이미 팔로우 중입니다.");
            return ResponseEntity.ok(response);
        }

        Follow follow = new Follow();
        follow.setFollower(userWithId(followerId));
        follow.setFollowing(userWithId(followingId));
        followRepository.save(follow);

        response.put("success", true);
        response.put("message", "팔로우 성공");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{followerId}/{followingId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> unfollow(@PathVariable Integer followerId, @PathVariable Integer followingId) {
        followRepository.deleteByFollowerAndFollowing(userWithId(followerId), userWithId(followingId));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "언팔로우 성공");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/followers/{userId}")
    public List<Follow> getFollowers(@PathVariable Integer userId) {
        return followRepository.findByFollowing(userWithId(userId));
    }

    @GetMapping("/following/{userId}")
    public List<Follow> getFollowing(@PathVariable Integer userId) {
        return followRepository.findByFollower(userWithId(userId));
    }

    private User userWithId(Integer id) {
        User user = new User();
        user.setUserNumber(id);
        return user;
    }
}
