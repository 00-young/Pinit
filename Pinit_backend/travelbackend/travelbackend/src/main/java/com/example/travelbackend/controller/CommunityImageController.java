package com.example.travelbackend.controller;

import com.example.travelbackend.entity.Community;
import com.example.travelbackend.entity.CommunityImage;
import com.example.travelbackend.repository.CommunityImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community-image")
@CrossOrigin(origins = "*")
public class CommunityImageController {

    @Autowired
    private CommunityImageRepository communityImageRepository;

    @GetMapping("/community/{communityNumber}")
    public List<CommunityImage> getImagesByCommunity(@PathVariable Integer communityNumber) {
        Community community = new Community();
        community.setCommunityNumber(communityNumber);
        return communityImageRepository.findByCommunity(community);
    }

    @PostMapping
    public CommunityImage addImage(@RequestBody CommunityImage image) {
        return communityImageRepository.save(image);
    }

    @DeleteMapping("/{id}")
    public void deleteImage(@PathVariable Integer id) {
        communityImageRepository.deleteById(id);
    }
}
