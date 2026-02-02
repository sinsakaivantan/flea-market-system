package com.example.fleamarketsystem.service;

import com.example.fleamarketsystem.entity.FavoriteItem;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.FavoriteItemRepository;
import com.example.fleamarketsystem.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteItemRepository favoriteItemRepository;
    private final ItemRepository itemRepository;

    public FavoriteService(FavoriteItemRepository favoriteItemRepository, ItemRepository itemRepository) {
        this.favoriteItemRepository = favoriteItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public FavoriteItem addFavorite(User user, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (favoriteItemRepository.existsByUserAndItem(user, item)) {
            throw new IllegalStateException("Item is already favorited by this user.");
        }

        FavoriteItem favoriteItem = new FavoriteItem();
        favoriteItem.setUser(user);
        favoriteItem.setItem(item);
        return favoriteItemRepository.save(favoriteItem);
    }

    @Transactional
    public void removeFavorite(User user, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        FavoriteItem favoriteItem = favoriteItemRepository.findByUserAndItem(user, item)
                .orElseThrow(() -> new IllegalStateException("Favorite not found."));

        favoriteItemRepository.delete(favoriteItem);
    }

    public boolean isFavorited(User user, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        return favoriteItemRepository.existsByUserAndItem(user, item);
    }

    public List<Item> getFavoriteItemsByUser(User user) {
        return favoriteItemRepository.findByUser(user).stream()
                .map(FavoriteItem::getItem)
                .collect(Collectors.toList());
    }

    public long getFavoriteCountByItem(Item item) {
        return favoriteItemRepository.countByItem(item);
    }
}
