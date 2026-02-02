// src/main/java/com/example/fleamarketsystem/repository/UserComplaintRepository.java
package com.example.fleamarketsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserComplaint;

public interface UserComplaintRepository extends JpaRepository<UserComplaint, Long> {
	long countByReportedUserId(User reportedUserId);

	List<UserComplaint> findByReportedUserIdOrderByCreatedAtDesc(User reportedUserId);

	List<UserComplaint> findAllByOrderByCreatedAtDesc();
}
