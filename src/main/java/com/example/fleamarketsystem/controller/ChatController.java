package com.example.fleamarketsystem.controller;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.ChatService;
import com.example.fleamarketsystem.service.FavoriteService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.ReviewService;
import com.example.fleamarketsystem.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final ItemService itemService;
    private final UserService userService;
    private final FavoriteService favoriteService;
    private final ReviewService reviewService;

    public ChatController(ChatService chatService, ItemService itemService, UserService userService,
                         FavoriteService favoriteService, ReviewService reviewService) {
        this.chatService = chatService;
        this.itemService = itemService;
        this.userService = userService;
        this.favoriteService = favoriteService;
        this.reviewService = reviewService;
    }

    @GetMapping("/{itemId}")
    public String showChatScreen(@PathVariable("itemId") Long itemId, 
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        var item = itemService.getItemById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        model.addAttribute("item", item);
        model.addAttribute("chats", chatService.getChatMessagesByItem(itemId));

        // Add seller's average rating
        reviewService.getAverageRatingForSeller(item.getSeller())
                .ifPresent(avg -> model.addAttribute("sellerAverageRating", String.format("%.1f", avg)));

        // Add favorite status
        if (userDetails != null) {
            User currentUser = userService.getUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            model.addAttribute("isFavorited", favoriteService.isFavorited(currentUser, itemId));
        }

        return "item_detail"; // Re-use item_detail for chat display
    }

    @PostMapping("/{itemId}")
    public String sendMessage(
            @PathVariable("itemId") Long itemId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("message") String message) {
        User sender = userService.getUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        chatService.sendMessage(itemId, sender, message);
        return "redirect:/chat/{itemId}";
    }
}