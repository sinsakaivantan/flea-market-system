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

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.ShootingGameService;
import com.example.fleamarketsystem.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/shooting")
@RequiredArgsConstructor
public class ShootingGameApiController {

    private final ShootingGameService gameService;
    private final UserService userService;

    // スコア送信
    @PostMapping("/score")
    public ResponseEntity<?> submitScore(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, Integer> payload) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        gameService.saveScore(user, payload.get("score"));
        return ResponseEntity.ok().build();
    }

    // ランキング取得
    @GetMapping("/ranking")
    public ResponseEntity<List<Map<String, Object>>> getRanking() {
        List<Map<String, Object>> ranking = gameService.getRanking().stream().map(s -> Map.of(
            "username", (Object)s.getUser().getName(), // またはニックネーム
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
}