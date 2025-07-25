package com.example.homework.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.homework.entity.Assignment;
import com.example.homework.entity.Submission;
import com.example.homework.entity.User;
import com.example.homework.repository.AssignmentRepository;
import com.example.homework.repository.SubmissionRepository;
import com.example.homework.repository.UserRepository;
import com.example.homework.security.JwtUtil;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ STUDENT SUBMITS HOMEWORK
    @PostMapping("/student/submit")
    public ResponseEntity<?> submitHomework(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assignmentId") Long assignmentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
            if (assignment == null) {
                return ResponseEntity.badRequest().body("Invalid assignment ID");
            }

            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.getUsernameFromToken(token);

            User student = userRepository.findByUsername(username);
            if (student == null) {
                return ResponseEntity.status(401).body("Unauthorized student");
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get("uploads/" + fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());

            Submission submission = new Submission();
            submission.setAssignment(assignment);
            submission.setStudent(student);
            submission.setFilePath(filePath.toString());
            submission.setSubmittedAt(LocalDateTime.now());
            submissionRepository.save(submission);

            return ResponseEntity.ok("Submission successful");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
        }
    }

    // ✅ TEACHER ADDS FEEDBACK TO A SUBMISSION
    @PutMapping("/teacher/submissions/{id}/feedback")
    public ResponseEntity<?> addCommentToSubmission(
        @PathVariable Long id,
        @RequestBody Map<String, Object> payload,
        @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.getRoleFromToken(token).equals("TEACHER")) {
            return ResponseEntity.status(403).body("Only teachers can submit feedback");
        }

        Submission submission = submissionRepository.findById(id).orElse(null);
        if (submission == null) {
            return ResponseEntity.badRequest().body("Submission not found");
        }

        String feedback = (String) payload.get("feedback");
        Double score = payload.get("score") != null ? Double.valueOf(payload.get("score").toString()) : null;

        submission.setFeedback(feedback);
        submission.setScore(score);
        submissionRepository.save(submission);

        return ResponseEntity.ok(submission);
    }

    @GetMapping("/student/submissions")
    public ResponseEntity<?> getStudentSubmissions(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);

        User student = userRepository.findByUsername(username);
        if (student == null) {
            return ResponseEntity.status(401).body("Unauthorized student");
        }

        List<Submission> submissions = submissionRepository.findByStudent(student);
        return ResponseEntity.ok(submissions);
    }

}
