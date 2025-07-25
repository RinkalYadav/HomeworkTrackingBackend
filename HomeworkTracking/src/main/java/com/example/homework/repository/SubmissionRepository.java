package com.example.homework.repository;
import com.example.homework.entity.Submission;
import com.example.homework.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
	List<Submission> findByAssignmentId(Long assignmentId);

	List<Submission> findByStudent(User student);
}