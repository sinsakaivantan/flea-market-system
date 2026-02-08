package com.example.fleamarketsystem.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    
    // --- 既存の単純な検索 ---
    Page<Item> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);
    Page<Item> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndCategoryIdAndStatus(String name, Long categoryId, String status, Pageable pageable);
    Page<Item> findByStatus(String status, Pageable pageable);

    // --- (旧) BANフラグと有効フラグのみを見る検索 ---
    Page<Item> findByStatusAndSeller_BannedFalseAndSeller_EnabledTrue(String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(String name, String status, Pageable pageable);
    Page<Item> findByCategoryIdAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(Long categoryId, String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndCategoryIdAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(String name, Long categoryId, String status, Pageable pageable);

    // --- 【今回追加】一時停止ユーザーが0人の時に使うメソッド (Serviceから呼ばれていたもの) ---
    // ※これらが足りていなかったためエラーになっていました
    Page<Item> findByStatusAndSeller_EnabledTrue(String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndStatusAndSeller_EnabledTrue(String name, String status, Pageable pageable);
    Page<Item> findByCategoryIdAndStatusAndSeller_EnabledTrue(Long categoryId, String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndCategoryIdAndStatusAndSeller_EnabledTrue(String name, Long categoryId, String status, Pageable pageable);

    // --- 【今回追加】一時停止ユーザーを除外(NotIn)して検索するメソッド ---
    Page<Item> findByStatusAndSeller_EnabledTrueAndSeller_IdNotIn(String status, Collection<Long> bannedSellerIds, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndStatusAndSeller_EnabledTrueAndSeller_IdNotIn(String name, String status, Collection<Long> bannedSellerIds, Pageable pageable);
    Page<Item> findByCategoryIdAndStatusAndSeller_EnabledTrueAndSeller_IdNotIn(Long categoryId, String status, Collection<Long> bannedSellerIds, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndCategoryIdAndStatusAndSeller_EnabledTrueAndSeller_IdNotIn(String name, Long categoryId, String status, Collection<Long> bannedSellerIds, Pageable pageable);


    // --- その他のメソッド ---
    List<Item> findBySeller(User seller);
    List<Item> findBySellerAndStatus(User seller, String status);
    long countBySeller(User seller);
}