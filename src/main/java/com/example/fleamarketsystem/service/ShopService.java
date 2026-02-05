package com.example.fleamarketsystem.service;

import com.example.fleamarketsystem.entity.PlayerLoadout;
import com.example.fleamarketsystem.entity.ShopItem;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserInventory;
import com.example.fleamarketsystem.repository.PlayerLoadoutRepository;
import com.example.fleamarketsystem.repository.ShopItemRepository;
import com.example.fleamarketsystem.repository.UserInventoryRepository;
// ★★★ 以下の2行がJSON読み込みに必須です ★★★
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopItemRepository itemRepository;
    private final UserInventoryRepository inventoryRepository;
    private final PlayerLoadoutRepository loadoutRepository;
    private final UserMoneyService userMoneyService;

    // --- JSONからデータを読み込んでDB登録 ---
    @EventListener(ApplicationReadyEvent.class)
    public void initShopData() {
        try {
            // geminigameitem.json を読み込む
            ClassPathResource resource = new ClassPathResource("geminigameitem.json");
            if (!resource.exists()) {
                System.out.println("geminigameitem.jsonが見つかりません");
                return;
            }

            // Jackson (JSONパーサー) を使って読み込む
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = resource.getInputStream();
            
            // ★ここがエラーの発生箇所でした（TypeReferenceのインポート漏れ）
            List<ShopItem> items = mapper.readValue(inputStream, new TypeReference<List<ShopItem>>(){});

            for (ShopItem jsonItem : items) {
                // 名前で既存データを検索
                ShopItem dbItem = itemRepository.findByName(jsonItem.getName());
                if (dbItem == null) {
                    dbItem = new ShopItem();
                    dbItem.setName(jsonItem.getName());
                }

                // データを上書き
                dbItem.setDescription(jsonItem.getDescription());
                dbItem.setPrice(jsonItem.getPrice());
                dbItem.setType(jsonItem.getType());
                
                dbItem.setFireRate(jsonItem.getFireRate());
                dbItem.setFirePattern(jsonItem.getFirePattern());
                dbItem.setBulletCount(jsonItem.getBulletCount());
                dbItem.setBulletSize(jsonItem.getBulletSize());
                
                dbItem.setBonusHp(jsonItem.getBonusHp());
                dbItem.setDamageReduction(jsonItem.getDamageReduction());
                
                dbItem.setRotationSpeed(jsonItem.getRotationSpeed());
                dbItem.setSpeedForward(jsonItem.getSpeedForward());
                dbItem.setSpeedBackward(jsonItem.getSpeedBackward());
                
                dbItem.setUltType(jsonItem.getUltType());
                dbItem.setUltChargeReq(jsonItem.getUltChargeReq());
                dbItem.setUltPower(jsonItem.getUltPower());
                
                dbItem.setImageUrl(jsonItem.getImageUrl());

                itemRepository.save(dbItem);
            }
            
            System.out.println("JSONアイテムデータのロード完了: " + items.size() + "件");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 以下、既存機能 ---

    public List<ShopItem> getAllItems() {
        return itemRepository.findAll();
    }

    public List<UserInventory> getUserInventory(User user) {
        return inventoryRepository.findByUser(user);
    }

    @Transactional
    public void buyItem(User user, Long itemId) {
        ShopItem item = itemRepository.findById(itemId).orElseThrow();
        if (inventoryRepository.existsByUserAndItem(user, item)) {
            throw new RuntimeException("既に持っています");
        }
        userMoneyService.spend(user, item.getPrice());
        UserInventory inventory = new UserInventory();
        inventory.setUser(user);
        inventory.setItem(item);
        inventory.setEquipped(false);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void equipItem(User user, Long itemId) {
        ShopItem newItem = itemRepository.findById(itemId).orElseThrow();
        List<UserInventory> inventoryList = inventoryRepository.findByUser(user);
        for (UserInventory inv : inventoryList) {
            if (inv.getItem().getType() == newItem.getType()) {
                inv.setEquipped(false);
                inventoryRepository.save(inv);
            }
        }
        UserInventory targetInv = inventoryRepository.findByUserAndItem(user, newItem);
        if (targetInv != null) {
            targetInv.setEquipped(true);
            inventoryRepository.save(targetInv);
        }
        recalculateLoadout(user);
    }

    @Transactional
    public void unequipItem(User user, Long itemId) {
        ShopItem item = itemRepository.findById(itemId).orElseThrow();
        UserInventory targetInv = inventoryRepository.findByUserAndItem(user, item);
        if (targetInv != null && targetInv.isEquipped()) {
            targetInv.setEquipped(false);
            inventoryRepository.save(targetInv);
            recalculateLoadout(user);
        }
    }

    private void recalculateLoadout(User user) {
        PlayerLoadout loadout = loadoutRepository.findByUserId(user.getId());
        
        loadout.setFireRate(10.0);
        loadout.setFirePattern("SPREAD");
        loadout.setBulletCount(1);
        loadout.setBulletSize(1.0);
        loadout.setBonusHp(0);
        loadout.setDamageReduction(0.0);
        loadout.setRotationSpeed(0.08);
        loadout.setSpeedForward(5.0);
        loadout.setSpeedBackward(2.5);
        loadout.setUltType("NONE");
        loadout.setUltChargeReq(10);
        loadout.setUltPower(0.0);

        List<UserInventory> inventoryList = inventoryRepository.findByUser(user);
        for (UserInventory inv : inventoryList) {
            if (inv.isEquipped()) {
                applyItemStats(loadout, inv.getItem());
            }
        }
        loadoutRepository.save(loadout);
    }

    private void applyItemStats(PlayerLoadout loadout, ShopItem item) {
        if (item.getType() == ShopItem.ItemType.WEAPON) {
            if (item.getFireRate() != null) loadout.setFireRate(item.getFireRate());
            if (item.getFirePattern() != null) loadout.setFirePattern(item.getFirePattern());
            if (item.getBulletCount() != null) loadout.setBulletCount(item.getBulletCount());
            if (item.getBulletSize() != null) loadout.setBulletSize(item.getBulletSize());
        }
        if (item.getType() == ShopItem.ItemType.AMULET) {
            if (item.getBonusHp() != null) loadout.setBonusHp(item.getBonusHp());
            if (item.getDamageReduction() != null) loadout.setDamageReduction(item.getDamageReduction());
        }
        if (item.getType() == ShopItem.ItemType.DOPING) {
            if (item.getRotationSpeed() != null) loadout.setRotationSpeed(item.getRotationSpeed());
            if (item.getSpeedForward() != null) loadout.setSpeedForward(item.getSpeedForward());
            if (item.getSpeedBackward() != null) loadout.setSpeedBackward(item.getSpeedBackward());
        }
        if (item.getType() == ShopItem.ItemType.ULT) {
            if (item.getUltType() != null) loadout.setUltType(item.getUltType());
            if (item.getUltChargeReq() != null) loadout.setUltChargeReq(item.getUltChargeReq());
            if (item.getUltPower() != null) loadout.setUltPower(item.getUltPower());
        }
    }
}