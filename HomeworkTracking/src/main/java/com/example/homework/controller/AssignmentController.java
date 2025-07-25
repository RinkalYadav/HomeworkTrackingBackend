package com.example.homework.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.homework.entity.Assignment;
import com.example.homework.repository.AssignmentRepository;
import com.example.homework.repository.SubmissionRepository; // ✅ Add this

@RestController
@RequestMapping("/api/teacher")
public class AssignmentController {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository; // ✅ Inject this

    @PostMapping("/assignments")
    public ResponseEntity<?> createAssignment(@RequestBody Assignment assignment) {
    	 // ✅ If teacher is assigned, set teacherName string from teacher object
        if (assignment.getTeacher() != null) {
            assignment.setTeacherName(assignment.getTeacher().getUsername()); // or getFullName() if you have it
        }
        Assignment savedAssignment = assignmentRepository.save(assignment);
        return ResponseEntity.ok(savedAssignment);
    }

    @GetMapping("/assignments")
    public ResponseEntity<?> getAssignments() {
        return ResponseEntity.ok(assignmentRepository.findAll());
    }

    @GetMapping("/submissions/{assignmentId}")  // ✅ Fixed endpoint path
    public ResponseEntity<?> getSubmissionsForAssignment(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(submissionRepository.findByAssignmentId(assignmentId));
    }
}
