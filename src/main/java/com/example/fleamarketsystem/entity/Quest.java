package com.example.fleamarketsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "quests")
@Data
public class Quest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    private String title;
    private String description;
    
    @Column(name = "quest_type")
    private String questType; // "NORMAL" or "DAILY"
    
    @Column(name = "condition_code")
    private String conditionCode;
    
    @Column(name = "reward_amount")
    private Integer rewardAmount;
}