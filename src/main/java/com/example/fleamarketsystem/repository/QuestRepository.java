package com.example.fleamarketsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.Quest;

public interface QuestRepository extends JpaRepository<Quest, Integer> {
    Optional<Quest> findByConditionCode(String conditionCode);
}