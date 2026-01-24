package com.example.fleamarketsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.Admin;
import com.example.fleamarketsystem.entity.User;

public interface AdminRepository extends JpaRepository<Admin, Long> {
	List<Admin> findAllByOrderByTimeDesc();
	List<Admin> findByUserId(User userId);
}