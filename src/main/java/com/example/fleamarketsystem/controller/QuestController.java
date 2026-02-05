package com.example.fleamarketsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.QuestService;
import com.example.fleamarketsystem.service.UserMoneyService; // ★追加
import com.example.fleamarketsystem.service.UserService;

@Controller
@RequestMapping("/quests")
public class QuestController {
    
    @Autowired private QuestService questService;
    @Autowired private UserService userService;
    @Autowired private UserMoneyService userMoneyService; // ★追加

    @GetMapping
    public String questList(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        
        // 1. クエスト一覧を渡す
        model.addAttribute("userQuests", questService.getUserQuests(user.getId()));
        
        // 2. ★追加: 現在の所持コイン数を取得して画面に渡す
        Integer currentCoins = userMoneyService.getFleaCoin(user);
        model.addAttribute("currentCoins", currentCoins);
        
        return "quest_list";
    }
    
    @PostMapping("/claim/{id}")
    @ResponseBody
    public ResponseEntity<?> claim(@PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
        
        boolean success = questService.claimReward(user.getId(), id);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}