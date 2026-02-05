package com.example.fleamarketsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.ShopItem;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long>{
	ShopItem findByName(String name);
}
