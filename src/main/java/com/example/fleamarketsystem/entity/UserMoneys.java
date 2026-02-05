package com.example.fleamarketsystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "user_moneys")
@Data
public class UserMoneys {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private Integer singularityToken; // 所持ST
    
    private Integer fleaCoin = 0;
    
    // コンストラクタ（初期化用）
    public UserMoneys() {
        this.singularityToken = 0; // デフォルト0
    }
}