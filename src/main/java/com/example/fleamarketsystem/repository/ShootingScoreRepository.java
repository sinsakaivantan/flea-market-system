package com.example.fleamarketsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.ShootingScore;

public interface ShootingScoreRepository extends JpaRepository<ShootingScore, Long> {
    // スコアの高い順にトップ10を取得
    List<ShootingScore> findTop10ByOrderByScoreDesc();
    
    // ユーザーIDで検索
    ShootingScore findByUserId(Long userId);
}