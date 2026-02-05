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
@Table(name = "player_loadouts")
@Data
public class PlayerLoadout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    // --- 武器 (Weapon) ---
    private double fireRate;       // 1秒間の発射数 (例: 10.0)
    private int bulletCount;       // 一度に打つ数/方向 (例: 1=直線, 3=3way)
    private String firePattern;
    private double bulletSize;     // 弾の大きさ倍率 (例: 1.0)

    // --- お守り (Amulet) ---
    private int bonusHp;           // 追加HP (例: 50)
    private double damageReduction;// ダメージ軽減率 (0.0 ~ 1.0, 例: 0.1 = 10%カット)

    // --- ドーピング剤 (Doping) ---
    private double rotationSpeed;  // 回転速度 (例: 0.08)
    private double speedForward;   // 前進速度 (例: 5.0)
    private double speedBackward;  // 後退速度 (例: 2.5)

    // --- 必殺技 (Ultimate) ---
    // 種類: "WIPE"(全滅), "INVINCIBLE"(無敵), "HEAL"(回復)
    private String ultType;        
    private int ultChargeReq;      // 必要撃破数 (例: 10)
    private double ultPower;       // 効果量 (無敵秒数 or 回復量)
}