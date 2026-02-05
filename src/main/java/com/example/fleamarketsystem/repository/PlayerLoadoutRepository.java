package com.example.fleamarketsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.PlayerLoadout;

public interface PlayerLoadoutRepository extends JpaRepository<PlayerLoadout, Long> {
    PlayerLoadout findByUserId(Long userId);
}