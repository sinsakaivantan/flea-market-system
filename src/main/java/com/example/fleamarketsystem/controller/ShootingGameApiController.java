package com.example.fleamarketsystem.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fleamarketsystem.entity.PlayerLoadout;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.QuestService; // ★追加
import com.example.fleamarketsystem.service.ShootingGameService;
import com.example.fleamarketsystem.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shooting")
@RequiredArgsConstructor
public class ShootingGameApiController {

    private final ShootingGameService gameService;
    private final UserService userService;
    private final QuestService questService; // ★追加: これで自動的に注入されます

    // スコア送信
    @PostMapping("/score")
    public ResponseEntity<?> submitScore(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, Integer> payload) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        
        Integer score = payload.get("score");
        gameService.saveScore(user, score);

        // ★追加: クエスト判定
        // すでに user オブジェクトを持っているので、そこから ID を取得します
        if (score != null && score >= 100) {
            questService.checkAndCompleteQuest(user.getId(), "SHOOTING_SCORE_100");
        }

        return ResponseEntity.ok().build();
    }

    // ランキング取得
    @GetMapping("/ranking")
    public ResponseEntity<List<Map<String, Object>>> getRanking() {
        List<Map<String, Object>> ranking = gameService.getRanking().stream().map(s -> Map.of(
            "username", (Object)s.getUser().getName(), 
            "score", (Object)s.getScore()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(ranking);
    }
    
    // 自分のベストスコア取得
    @GetMapping("/my-best")
    public ResponseEntity<Integer> getMyBest(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(gameService.getMyBestScore(user));
    }
    
    // 装備データ取得
    @GetMapping("/loadout")
    public ResponseEntity<PlayerLoadout> getLoadout(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(gameService.getOrCreateLoadout(user));
    }
}