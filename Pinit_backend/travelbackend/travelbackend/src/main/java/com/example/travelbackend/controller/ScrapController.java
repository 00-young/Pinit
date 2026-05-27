package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.entity.Scrap;
import com.example.travelbackend.entity.User;
import com.example.travelbackend.repository.ScrapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/scrap")
@CrossOrigin(origins = "*")
public class ScrapController {

    @Autowired
    private ScrapRepository scrapRepository;

    @PostMapping("/{userNumber}/{communityNumber}")
    public ResponseEntity<Map<String, Object>> addScrap(@PathVariable Integer userNumber, @PathVariable Integer communityNumber) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Scrap> existing = scrapRepository.findByUserAndCommunity(
                userWithId(userNumber), communityWithId(communityNumber));
        
        if (existing.isPresent()) {
            response.put("success", false);
            response.put("message", "이미 스크랩한 게시글입니다.");
            return ResponseEntity.ok(response);
        }

        Scrap scrap = new Scrap();
        scrap.setUser(userWithId(userNumber));
        scrap.setCommunity(communityWithId(communityNumber));
        scrapRepository.save(scrap);

        response.put("success", true);
        response.put("message", "스크랩 성공");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userNumber}/{communityNumber}")
    @Transactional
    public ResponseEntity<Map<String, Object>> removeScrap(@PathVariable Integer userNumber, @PathVariable Integer communityNumber) {
        scrapRepository.deleteByUserAndCommunity(userWithId(userNumber), communityWithId(communityNumber));
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "스크랩 취소 성공");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userNumber}")
    public List<Scrap> getUserScraps(@PathVariable Integer userNumber) {
        return scrapRepository.findByUser(userWithId(userNumber));
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
