package com.example.fleamarketsystem.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.fleamarketsystem.entity.Admin;
import com.example.fleamarketsystem.repository.AdminRepository;


@Service
public class RenrakuService {
	
	private final AdminRepository adminRepository;

	public RenrakuService(AdminRepository adminRepository) {
		this.adminRepository = adminRepository;
	}
	
	public List<Admin> getAllAdmin(){
		return adminRepository.findAllByOrderByTimeDesc();
	}
}