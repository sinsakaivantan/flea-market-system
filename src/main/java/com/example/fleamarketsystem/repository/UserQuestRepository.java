package com.example.fleamarketsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.Quest;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserQuest;

public interface UserQuestRepository extends JpaRepository<UserQuest, Integer> {
    
    Optional<UserQuest> findByUserAndQuest(User user, Quest quest);
    
    List<UserQuest> findByUser(User user);
    
    @Modifying
    @Transactional
    @Query("UPDATE UserQuest uq SET uq.isCompleted = false, uq.isClaimed = false, uq.completedAt = null " +
           "WHERE uq.quest.id IN (SELECT q.id FROM Quest q WHERE q.questType = 'DAILY')")
    void resetDailyQuests();
}