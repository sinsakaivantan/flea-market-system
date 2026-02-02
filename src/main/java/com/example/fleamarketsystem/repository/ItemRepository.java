package com.example.fleamarketsystem.repository;

import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Page<Item> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);
    Page<Item> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndCategoryIdAndStatus(String name, Long categoryId, String status, Pageable pageable);
    Page<Item> findByStatus(String status, Pageable pageable);

    /** 出品者がBANでなく有効なアカウントの商品のみ（商品一覧用） */
    Page<Item> findByStatusAndSeller_BannedFalseAndSeller_EnabledTrue(String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(String name, String status, Pageable pageable);
    Page<Item> findByCategoryIdAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(Long categoryId, String status, Pageable pageable);
    Page<Item> findByNameContainingIgnoreCaseAndCategoryIdAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(String name, Long categoryId, String status, Pageable pageable);

    List<Item> findBySeller(User seller);
    List<Item> findBySellerAndStatus(User seller, String status);
    long countBySeller(User seller);
}
