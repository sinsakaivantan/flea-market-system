package com.example.fleamarketsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.Ban;
import com.example.fleamarketsystem.entity.User;


	@Repository
	public interface BanRepository extends JpaRepository<Ban, Long> {
		Optional<Ban> findTopByUserIdOrderByEndDesc(User useId);
		List<Ban> findAllByUserId(User userId);
	}

