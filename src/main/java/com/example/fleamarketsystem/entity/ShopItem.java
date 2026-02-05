package com.example.fleamarketsystem.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ShopItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Integer price; // シンギュラリティトークン価格
    
    @Enumerated(EnumType.STRING)
    private ItemType type; // WEAPON, AMULET, DOPING, ULT

    // --- 反映させるパラメータ (nullの場合は反映しない) ---
    private Double fireRate;
    private String firePattern; // "SPREAD", "FRONT_BACK" など
    private Integer bulletCount;
    private Double bulletSize;
    
    private Integer bonusHp;
    private Double damageReduction;
    
    private Double rotationSpeed;
    private Double speedForward;
    private Double speedBackward;
    
    private String ultType;
    private Integer ultChargeReq;
    private Double ultPower;

    private String imageUrl;
    
    public enum ItemType { WEAPON, AMULET, DOPING, ULT }
}