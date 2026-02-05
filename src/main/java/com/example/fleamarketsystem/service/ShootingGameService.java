package com.example.fleamarketsystem.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.PlayerLoadout;
import com.example.fleamarketsystem.entity.ShootingScore;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.PlayerLoadoutRepository;
import com.example.fleamarketsystem.repository.ShootingScoreRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShootingGameService {

    private final ShootingScoreRepository repository;
    private final PlayerLoadoutRepository loadoutRepository;
    
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
    
    @Transactional
    public PlayerLoadout getOrCreateLoadout(User user) {
        PlayerLoadout loadout = loadoutRepository.findByUserId(user.getId());
        if (loadout == null) {
            loadout = new PlayerLoadout();
            loadout.setUser(user);
            
            // --- 初期ステータス設定 ---
            // 武器
            loadout.setFireRate(10.0);
            loadout.setBulletCount(1);
            loadout.setBulletSize(1.0);
            
            // お守り
            loadout.setBonusHp(0);
            loadout.setDamageReduction(0.0);
            
            // ドーピング
            loadout.setRotationSpeed(0.08);
            loadout.setSpeedForward(3.0);
            loadout.setSpeedBackward(2.0);
            
            // 必殺技 (初期は全滅ボム)
            loadout.setUltType("NONE");
            loadout.setUltChargeReq(1);
            loadout.setUltPower(0.0); // WIPEには効果量は不要だが一応0

            loadoutRepository.save(loadout);
        }
        return loadout;
    }

    public List<ShootingScore> getRanking() {
        return repository.findTop10ByOrderByScoreDesc();
    }
    
    public Integer getMyBestScore(User user) {
        ShootingScore score = repository.findByUserId(user.getId());
        return score != null ? score.getScore() : 0;
    }
}