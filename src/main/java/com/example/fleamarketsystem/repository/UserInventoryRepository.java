package com.example.fleamarketsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.ShopItem;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserInventory;

public interface UserInventoryRepository extends JpaRepository<UserInventory, Long>{
	List<UserInventory> findByUser(User user);

    // 特定のユーザーが特定のアイテムを持っているかチェック
    boolean existsByUserAndItem(User user, ShopItem item);

    // 特定のユーザーの特定アイテムのインベントリ情報を取得（装備変更用）
    UserInventory findByUserAndItem(User user, ShopItem item);
}
