package com.example.fleamarketsystem.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.fleamarketsystem.dto.QuestClaimRequest;
import com.example.fleamarketsystem.dto.QuestClaimResponse;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.QuestService;
import com.example.fleamarketsystem.service.UserMoneyService;
import com.example.fleamarketsystem.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class QuestWebSocketController {

    private final QuestService questService;
    private final UserService userService;
    private final UserMoneyService userMoneyService;
    private final SimpMessagingTemplate messagingTemplate;

    // クライアントからの "/app/claim" を受け取る
    @MessageMapping("/claim")
    public void handleClaimReward(QuestClaimRequest request, Principal principal) {
        try {
            // ログインユーザー特定
            User user = userService.getUserByEmail(principal.getName()).orElseThrow();

            // 報酬受取処理実行
            boolean success = questService.claimReward(user.getId(), request.getQuestId());

            if (success) {
                // 最新のコイン残高を取得
                Integer currentCoin = userMoneyService.getFleaCoin(user);
                
                // 成功レスポンスを作成
                QuestClaimResponse response = new QuestClaimResponse(true, request.getQuestId(), currentCoin, "報酬を受け取りました！");
                
                // ★特定のユーザーだけにメッセージを返信 ("/user/queue/reply" に届く)
                messagingTemplate.convertAndSendToUser(
                    principal.getName(), 
                    "/queue/reply", 
                    response
                );
            } else {
                sendError(principal.getName(), request.getQuestId(), "条件を満たしていないか、既に受取済みです。");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(principal.getName(), request.getQuestId(), "エラーが発生しました: " + e.getMessage());
        }
    }
    
    @MessageMapping("/slot-win")
    public void handleSlotWin(Principal principal) {
        try {
            User user = userService.getUserByEmail(principal.getName()).orElseThrow();

            // "SLOT_777" というコードのクエストを達成扱いにする
            questService.checkAndCompleteQuest(user.getId(), "SLOT_777");

            // ユーザーに「達成しました！」と通知を送り返す
            // (QuestService内で自動的に通知を送る仕組みがない場合、ここで手動で送る)
            QuestClaimResponse response = new QuestClaimResponse(true, 0, 0, "クエスト達成！報酬を受け取ってください。");
            
            messagingTemplate.convertAndSendToUser(
                principal.getName(), 
                "/queue/reply", 
                response
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendError(String username, Integer questId, String msg) {
        QuestClaimResponse response = new QuestClaimResponse(false, questId, 0, msg);
        messagingTemplate.convertAndSendToUser(username, "/queue/reply", response);
    }
}