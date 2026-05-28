package com.example.travelbackend.controller;

import com.example.travelbackend.entity.User;
import com.example.travelbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            // firebaseUid 중복 체크
            Optional<User> existing = userRepository.findByFirebaseUid(user.getFirebaseUid());
            if (existing.isPresent()) {
                result.put("success", false);
                result.put("message", "이미 사용 중인 아이디입니다.");
                return ResponseEntity.ok(result);
            }
            User saved = userRepository.save(user);
            result.put("success", true);
            result.put("message", "회원가입 성공!");
            result.put("userNumber", saved.getUserNumber());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "오류: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        try {
            String firebaseUid = request.get("firebaseUid");

            Optional<User> userOpt = userRepository.findByFirebaseUid(firebaseUid);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "존재하지 않는 유저입니다.");
                return ResponseEntity.ok(result);
            }

            User user = userOpt.get();
            // 비밀번호 체크 로직 제거 (Firebase 인증 사용)

            result.put("success", true);
            result.put("message", "로그인 성공!");
            result.put("userNumber", user.getUserNumber());
            result.put("userName", user.getUserName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "오류: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    // 전체 유저 조회
    @GetMapping
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 유저 삭제
    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Integer id) {
        userRepository.deleteById(id);
    }
}