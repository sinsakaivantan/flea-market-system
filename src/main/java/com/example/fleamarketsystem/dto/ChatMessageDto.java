package com.example.fleamarketsystem.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class ChatMessageDto {
    private Long id;
    private Long itemId;
    private String senderName;
    private String senderEmail;
    private String message;
    
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm")
    private LocalDateTime createdAt;
    
    // 自分のメッセージかどうか判定するためのフラグ (フロントで設定する場合もあるが、ここではEmailで判定)
}