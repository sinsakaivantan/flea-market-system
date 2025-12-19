// src/main/java/com/example/fleamarketsystem/repository/UserComplaintRepository.javas
package com.example.fleamarketsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.UserComplaint;

public interface UserComplaintRepository extends JpaRepository<UserComplaint, Long> {
	long countByReportedUserId(Long reportedUserId);

	List<UserComplaint> findByReportedUserIdOrderByCreatedAtDesc(Long reportedUserId);
}
