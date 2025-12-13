package com.example.fleamarketsystem.service;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.example.fleamarketsystem.entity.Admin;
import com.example.fleamarketsystem.repository.AdminRepository;

@Service
public class ReportService {
	private final AdminRepository adminRepository;
	public ReportService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }
	 public Admin saveAdmin(Admin admin) throws IOException {
	        return adminRepository.save(admin);
	    }
}
