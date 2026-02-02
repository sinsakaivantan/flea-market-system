package com.example.fleamarketsystem.service;

import com.example.fleamarketsystem.entity.Chat;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.ChatRepository;
import com.example.fleamarketsystem.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final ItemRepository itemRepository;
    private final EmailService emailService;

    public ChatService(ChatRepository chatRepository, ItemRepository itemRepository, EmailService emailService) {
        this.chatRepository = chatRepository;
        this.itemRepository = itemRepository;
        this.emailService = emailService;
    }

    public List<Chat> getChatMessagesByItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        return chatRepository.findByItemOrderByCreatedAtAsc(item);
    }

    public Chat sendMessage(Long itemId, User sender, String message) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        Chat chat = new Chat();
        chat.setItem(item);
        chat.setSender(sender);
        chat.setMessage(message);
        chat.setCreatedAt(LocalDateTime.now());

        Chat savedChat = chatRepository.save(chat);

        // 送信者が出品者の場合はメールを送らない
        if (item.getSeller().equals(sender)) {
            return savedChat;
        }

        // Send email notification to the other party (seller) when buyer sends a message
        User receiver = item.getSeller();
        if (receiver != null && receiver.getEmail() != null && !receiver.getEmail().isEmpty()) {
            String subject = String.format("商品「%s」に関する新しいメッセージ", item.getName());
            String notificationMessage = String.format("商品「%s」に関する新しいメッセージが届きました！\n\n送信者: %s\nメッセージ: %s",
                    item.getName(),
                    sender.getName(),
                    message);
            emailService.sendEmail(receiver.getEmail(), subject, notificationMessage);
        }

        return savedChat;
    }
}
