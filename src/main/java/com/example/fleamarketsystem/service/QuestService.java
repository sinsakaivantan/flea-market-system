package com.example.fleamarketsystem.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.Quest;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserQuest;
import com.example.fleamarketsystem.repository.QuestRepository;
import com.example.fleamarketsystem.repository.UserQuestRepository;
import com.example.fleamarketsystem.repository.UserRepository;

@Service
public class QuestService {

    @Autowired private QuestRepository questRepository;
    @Autowired private UserQuestRepository userQuestRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserMoneyService userMoneyService;

    // ... getUserQuests は変更なし ...
    public List<UserQuest> getUserQuests(Long userId) {
        if (!userRepository.existsById(userId)) return new ArrayList<>();
        User user = userRepository.findById(userId).orElse(null);

        List<Quest> allQuests = questRepository.findAll();
        List<UserQuest> userProgress = userQuestRepository.findByUser(user);
        
        Map<Integer, UserQuest> progressMap = userProgress.stream()
                .collect(Collectors.toMap(uq -> uq.getQuest().getId(), Function.identity()));

        List<UserQuest> result = new ArrayList<>();

        for (Quest quest : allQuests) {
            if (progressMap.containsKey(quest.getId())) {
                result.add(progressMap.get(quest.getId()));
            } else {
                UserQuest dummy = new UserQuest();
                dummy.setQuest(quest);
                dummy.setIsCompleted(false);
                dummy.setIsClaimed(false);
                result.add(dummy);
            }
        }
        return result;
    }

    // ... checkAndCompleteQuest も saveAndFlush にしておくと安心 ...
    @Transactional
    public void checkAndCompleteQuest(Long userId, String conditionCode) {
        Quest quest = questRepository.findByConditionCode(conditionCode).orElse(null);
        if (quest == null) return;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        UserQuest userQuest = userQuestRepository.findByUserAndQuest(user, quest)
                .orElse(new UserQuest());
        
        if (userQuest.getId() == null) {
            userQuest.setUser(user);
            userQuest.setQuest(quest);
        }

        if (Boolean.TRUE.equals(userQuest.getIsCompleted())) return;

        userQuest.setIsCompleted(true);
        userQuest.setCompletedAt(LocalDateTime.now());
        
        // 即時反映
        userQuestRepository.saveAndFlush(userQuest);
    }

    /**
     * 報酬受取
     */
    @Transactional
    public boolean claimReward(Long userId, Integer userQuestId) {
        // 1. データの取得
        UserQuest uq = userQuestRepository.findById(userQuestId).orElse(null);
        
        // バリデーション
        if (uq == null) return false;
        // IDの型比較 (longValueで統一)
        if (uq.getUser().getId().longValue() != userId.longValue()) return false;
        if (!Boolean.TRUE.equals(uq.getIsCompleted())) return false;
        if (Boolean.TRUE.equals(uq.getIsClaimed())) return false;

        // 2. フラグ更新
        uq.setIsClaimed(true);
        
        // ★修正: saveAndFlushで「今すぐ」DBに書き込む
        // これでLombokハッシュコード問題やトランザクション遅延による不整合を防げます
        userQuestRepository.saveAndFlush(uq);

        // 3. コイン付与
        // 最新のユーザー情報を再取得して渡す（UserMoneyServiceの安全のため）
        User currentUser = userRepository.findById(userId).orElseThrow();
        userMoneyService.addFleaCoin(currentUser, uq.getQuest().getRewardAmount());

        return true;
    }
}