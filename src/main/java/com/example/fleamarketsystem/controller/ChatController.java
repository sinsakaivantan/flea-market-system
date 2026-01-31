package com.example.fleamarketsystem.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.fleamarketsystem.dto.ChatMessageDto; // 作成したDTO
import com.example.fleamarketsystem.entity.Chat;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.ChatService;
import com.example.fleamarketsystem.service.FavoriteService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.ReviewService;
import com.example.fleamarketsystem.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ItemService itemService;
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final ReviewService reviewService;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket送信用のテンプレート

    @GetMapping("/{itemId}")
    public String showChatScreen(@PathVariable("itemId") Long itemId, 
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        // ... (既存のGET処理はそのまま維持) ...
        var item = itemService.getItemById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        model.addAttribute("item", item);
        model.addAttribute("chats", chatService.getChatMessagesByItem(itemId));
        
        // ログインユーザー情報をテンプレートに渡す（JSで自分のメッセージか判定するため）
        if (userDetails != null) {
             User currentUser = userService.getUserByEmail(userDetails.getUsername()).orElseThrow();
             model.addAttribute("currentUserEmail", currentUser.getEmail());
             // ... 他の既存処理 ...
        }
        return "item_detail";
    }

    // WebSocket経由でのメッセージ受信・配信
    @MessageMapping("/chat/{itemId}/send")
    public void sendMessage(@DestinationVariable Long itemId, 
                            @Payload ChatMessageDto messageDto, 
                            Principal principal) {
        
        // 1. 送信者を取得 (WebSocketセッションのPrincipalから)
        String email = principal.getName();
        User sender = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        // 2. DBに保存 (Service呼び出し)
        Chat savedChat = chatService.sendMessage(itemId, sender, messageDto.getMessage());

        // 3. レスポンス用DTOを作成
        ChatMessageDto responseDto = new ChatMessageDto();
        responseDto.setId(savedChat.getId());
        responseDto.setItemId(itemId);
        responseDto.setSenderName(sender.getName());
        responseDto.setSenderEmail(sender.getEmail());
        responseDto.setMessage(savedChat.getMessage());
        responseDto.setCreatedAt(savedChat.getCreatedAt());

        // 4. 購読しているクライアント全員に配信 (/topic/chat/{itemId})
        messagingTemplate.convertAndSend("/topic/chat/" + itemId, responseDto);
    }
}