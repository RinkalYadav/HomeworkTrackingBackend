package com.example.homework.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.homework.entity.User;
import com.example.homework.repository.UserRepository;
import com.example.homework.security.JwtUtil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        User foundUser = userRepository.findByUsername(user.getUsername());

        if (foundUser != null && passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "User has already registered");
            return ResponseEntity.ok(response);
        }

        // ✅ Role-based validation for 'classes'
        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            if (user.getClasses() == null || user.getClasses().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Teacher must select at least one class."));
            }
        } else if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            if (user.getClasses() == null || user.getClasses().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Student must select a class."));
            }
        }

        // ✅ Encode and save
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered");
        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        User foundUser = userRepository.findByUsername(user.getUsername());

        if (foundUser == null || !passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            return ResponseEntity.status(403).body("Invalid credentials");
        }

        if (!foundUser.getRole().equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body("Invalid role");
        }

        // ✅ CLASS VALIDATION for STUDENT
        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            if (user.getClasses() == null || !user.getClasses().equals(foundUser.getClasses())) {
                return ResponseEntity.status(403).body("Invalid class selected for student");
            }
        }

        // ✅ CLASS VALIDATION for TEACHER
        if ("TEACHER".equalsIgnoreCase(user.getRole())) {
            if (user.getClasses() == null || !Arrays.asList(foundUser.getClasses().split(",")).contains(user.getClasses())) {
                return ResponseEntity.status(403).body("Invalid class selected for teacher");
            }
        }

        // ✅ Generate JWT token
        String token = jwtUtil.generateToken(foundUser.getUsername(), foundUser.getRole());

        // ✅ Prepare a secure response (excluding password)
        Map<String, Object> response = new HashMap<>();
        response.put("token", token); // ← This is critical!
        response.put("username", foundUser.getUsername());
        response.put("role", foundUser.getRole());
        response.put("classes", foundUser.getClasses());

        return ResponseEntity.ok(response);
    }


//    @PostMapping("/login")
//    public ResponseEntity<?> login(@RequestBody User user) {
//        User foundUser = userRepository.findByUsername(user.getUsername());
//        if (foundUser != null && passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
//        	 if (!user.getRole().equalsIgnoreCase(foundUser.getRole())) {
//        	        return ResponseEntity.status(403).body("Invalid role selected");
//        	    }
//            String token = jwtUtil.generateToken(foundUser.getUsername(), foundUser.getRole());
//
//            // ✅ Return token, role, and username together as JSON
//            Map<String, Object> response = new HashMap<>();
//            response.put("token", token);
//            response.put("role", foundUser.getRole());
//            response.put("username", foundUser.getUsername());
//
//            return ResponseEntity.ok(response);
//        }
//
//        return ResponseEntity.status(401).body("Invalid credentials");
//    }

    
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // 🔐 Generate a reset token
        String resetToken = UUID.randomUUID().toString();

        // 👉 Save the token
        user.setResetToken(resetToken);
        userRepository.save(user);

        // ✅ Return token in response body as JSON
        Map<String, String> response = new HashMap<>();
        response.put("message", "Reset token generated");
        response.put("token", resetToken);
        return ResponseEntity.ok(response);

    }

    
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        User user = userRepository.findByResetToken(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null); // clear token after use
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password updated successfully");
        return ResponseEntity.ok(response);

    }
    @GetMapping("/student/profile")
    public ResponseEntity<?> getStudentProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.getUsernameFromToken(token);

            User student = userRepository.findByUsername(username);
            if (student == null || !student.getRole().equalsIgnoreCase("STUDENT")) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            // return username and class
            Map<String, String> response = new HashMap<>();
            response.put("username", student.getUsername());
            response.put("studentClass", student.getClasses());
  // or use student.getClasses()

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }



}



