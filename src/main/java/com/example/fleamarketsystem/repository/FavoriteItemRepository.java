package com.example.fleamarketsystem.repository;

import com.example.fleamarketsystem.entity.FavoriteItem;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    Optional<FavoriteItem> findByUserAndItem(User user, Item item);
    List<FavoriteItem> findByUser(User user);
    boolean existsByUserAndItem(User user, Item item);
    long countByItem(Item item);

    void deleteByItem_Seller(User seller);
}
