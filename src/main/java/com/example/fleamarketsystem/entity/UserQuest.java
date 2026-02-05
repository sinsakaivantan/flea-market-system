package com.example.fleamarketsystem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode; // ★追加

@Entity
@Table(name = "user_quests", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "quest_id"})
})
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // ★追加: ID以外が変わっても同一オブジェクトとみなす
public class UserQuest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include // ★追加: IDだけをハッシュの対象にする
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;
    
    @Column(name = "is_completed")
    private Boolean isCompleted = false;
    
    @Column(name = "is_claimed")
    private Boolean isClaimed = false;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}