package com.example.fleamarketsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.Ban;


	@Repository
	public interface BanRepository extends JpaRepository<Ban, Long> {
	}

