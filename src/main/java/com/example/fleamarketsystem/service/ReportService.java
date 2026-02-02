package com.example.fleamarketsystem.service;

import org.springframework.stereotype.Service;

import com.example.fleamarketsystem.entity.UserComplaint;
import com.example.fleamarketsystem.repository.UserComplaintRepository;

@Service
public class ReportService {
	private final UserComplaintRepository userComplaintRepository;

	public ReportService(UserComplaintRepository userComplaintRepository) {
		this.userComplaintRepository = userComplaintRepository;
	}

	public UserComplaint saveUserComplaint(UserComplaint complaint) {
		return userComplaintRepository.save(complaint);
	}
}
