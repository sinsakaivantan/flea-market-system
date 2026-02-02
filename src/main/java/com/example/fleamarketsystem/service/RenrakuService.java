package com.example.fleamarketsystem.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.fleamarketsystem.entity.Admin;
import com.example.fleamarketsystem.entity.UserComplaint;
import com.example.fleamarketsystem.repository.AdminRepository;
import com.example.fleamarketsystem.repository.UserComplaintRepository;


@Service
public class RenrakuService {
	
	private final AdminRepository adminRepository;
	private final UserComplaintRepository aaa;
	public RenrakuService(AdminRepository adminRepository, UserComplaintRepository aaa) {
		this.adminRepository = adminRepository;
		this.aaa=aaa;
	}
	
	public List<Admin> getAllAdmin(){
		return adminRepository.findAllByOrderByTimeDesc();
	}
	
	
	public List<UserComplaint> getAllUserComplaints(){
		return aaa.findAllByOrderByCreatedAtDesc();
	}
}