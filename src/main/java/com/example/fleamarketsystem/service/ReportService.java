package com.example.fleamarketsystem.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.example.fleamarketsystem.entity.Admin;
import com.example.fleamarketsystem.entity.UserComplaint;
import com.example.fleamarketsystem.repository.AdminRepository;
import com.example.fleamarketsystem.repository.UserComplaintRepository;

@Service
public class ReportService {
	private final UserComplaintRepository userComplaintRepository;
	private final AdminRepository adminRepository;
	public ReportService(UserComplaintRepository userComplaintRepository,AdminRepository adminRepository) {
		this.userComplaintRepository = userComplaintRepository;
		this.adminRepository = adminRepository;
	}

	public UserComplaint saveUserComplaint(UserComplaint complaint) {
		return userComplaintRepository.save(complaint);
	}
	
	public Admin saveAdmin(Admin admin) throws IOException {
        return adminRepository.save(admin);
    }
}
