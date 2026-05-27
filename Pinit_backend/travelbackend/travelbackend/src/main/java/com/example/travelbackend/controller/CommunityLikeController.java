package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.entity.CommunityLike;
import com.example.travelbackend.entity.User;
import com.example.travelbackend.repository.CommunityLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/like")
@CrossOrigin(origins = "*")
public class CommunityLikeController {

    @Autowired
    private CommunityLikeRepository communityLikeRepository;

    @PostMapping("/{userNumber}/{communityNumber}")
    public ResponseEntity<Map<String, Object>> addLike(@PathVariable Integer userNumber, @PathVariable Integer communityNumber) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<CommunityLike> existing = communityLikeRepository.findByUserAndCommunity(
                userWithId(userNumber), communityWithId(communityNumber));
        
        if (existing.isPresent()) {
            response.put("success", false);
            response.put("message", "이미 좋아요를 누른 게시글입니다.");
            return ResponseEntity.ok(response);
        }

        CommunityLike like = new CommunityLike();
        like.setUser(userWithId(userNumber));
        like.setCommunity(communityWithId(communityNumber));
        communityLikeRepository.save(like);

        response.put("success", true);
        response.put("message", "좋아요 성공");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userNumber}/{communityNumber}")
    @Transactional
    public ResponseEntity<Map<String, Object>> removeLike(@PathVariable Integer userNumber, @PathVariable Integer communityNumber) {
        communityLikeRepository.deleteByUserAndCommunity(userWithId(userNumber), communityWithId(communityNumber));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "좋아요 취소 성공");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userNumber}")
    public List<CommunityLike> getUserLikes(@PathVariable Integer userNumber) {
        return communityLikeRepository.findByUser(userWithId(userNumber));
    }

    private User userWithId(Integer id) {
        User user = new User();
        user.setUserNumber(id);
        return user;
    }

    private Community communityWithId(Integer id) {
        Community community = new Community();
        community.setCommunityNumber(id);
        return community;
    }
}
