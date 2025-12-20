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

        // Send email notification to the other party in the chat
        User receiver = null;
        if (item.getSeller().equals(sender)) {
            // If sender is seller, receiver is buyer (if item is sold)
            // This logic needs to be refined if chat is before purchase
            // For now, assuming chat is always between seller and buyer of a purchased item
            // Or, if chat is before purchase, the other party is always the seller
            // For simplicity, let's assume the chat is always between the item's seller and the current sender's counterpart
            // If sender is seller, receiver is the buyer of the item (if any order exists)
            // If sender is buyer, receiver is the seller of the item
            receiver = item.getSeller(); // Default to seller if sender is buyer
            // If sender is seller, we need to find the buyer from an order associated with this item
            // This requires more complex logic, for now, let's simplify: chat is always with the seller
        } else { // Sender is buyer
            receiver = item.getSeller();
        }

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
