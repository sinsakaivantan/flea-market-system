package com.example.fleamarketsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestClaimResponse {
    private boolean success;
    private Integer questId;
    private Integer newCoinBalance;
    private String message;
}