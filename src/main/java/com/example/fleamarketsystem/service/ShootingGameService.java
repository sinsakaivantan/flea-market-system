package com.example.fleamarketsystem.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.ShootingScore;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.ShootingScoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShootingGameService {

    private final ShootingScoreRepository repository;

    @Transactional
    public void saveScore(User user, int newScore) {
        ShootingScore current = repository.findByUserId(user.getId());
        
        if (current == null) {
            // 初プレイ
            current = new ShootingScore();
            current.setUser(user);
            current.setScore(newScore);
            current.setUpdatedAt(LocalDateTime.now());
            repository.save(current);
        } else {
            // ハイスコア更新時のみ保存
            if (newScore > current.getScore()) {
                current.setScore(newScore);
                current.setUpdatedAt(LocalDateTime.now());
                repository.save(current);
            }
        }
    }

    public List<ShootingScore> getRanking() {
        return repository.findTop10ByOrderByScoreDesc();
    }
    
    public Integer getMyBestScore(User user) {
        ShootingScore score = repository.findByUserId(user.getId());
        return score != null ? score.getScore() : 0;
    }
}