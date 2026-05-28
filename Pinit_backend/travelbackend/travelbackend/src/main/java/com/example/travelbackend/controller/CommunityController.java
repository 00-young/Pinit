package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.repository.CommunityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@CrossOrigin(origins = "*")
public class CommunityController {

    @Autowired
    private CommunityRepository communityRepository;

    @GetMapping
    public List<Community> getAllCommunities() {
        return communityRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Community> getCommunityById(@PathVariable Integer id) {
        return communityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Community createCommunity(@RequestBody Community community) {
        if (community.getCommunityDate() == null) {
            community.setCommunityDate(LocalDate.now());
        }
        return communityRepository.save(community);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Community> updateCommunity(@PathVariable Integer id, @RequestBody Community communityDetails) {
        return communityRepository.findById(id)
                .map(community -> {
                    community.setCommunityTitle(communityDetails.getCommunityTitle());
                    community.setCommunityContent(communityDetails.getCommunityContent());
                    community.setCommunityCategory(communityDetails.getCommunityCategory());
                    return ResponseEntity.ok(communityRepository.save(community));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCommunity(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        return communityRepository.findById(id)
                .map(community -> {
                    communityRepository.delete(community);
                    response.put("success", true);
                    response.put("message", "게시글이 삭제되었습니다.");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
