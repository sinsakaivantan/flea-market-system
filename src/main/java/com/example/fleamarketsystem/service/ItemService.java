package com.example.fleamarketsystem.service;

import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryService categoryService;
    private final CloudinaryService cloudinaryService;

    public ItemService(ItemRepository itemRepository, CategoryService categoryService, CloudinaryService cloudinaryService) {
        this.itemRepository = itemRepository;
        this.categoryService = categoryService;
        this.cloudinaryService = cloudinaryService;
    }

    public Page<Item> searchItems(String keyword, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (keyword != null && !keyword.isEmpty() && categoryId != null) {
            return itemRepository.findByNameContainingIgnoreCaseAndCategoryIdAndStatus(keyword, categoryId, "出品中", pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            return itemRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "出品中", pageable);
        } else if (categoryId != null) {
            return itemRepository.findByCategoryIdAndStatus(categoryId, "出品中", pageable);
        } else {
            return itemRepository.findByStatus("出品中", pageable);
        }
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public Item saveItem(Item item, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = cloudinaryService.uploadFile(imageFile);
            item.setImageUrl(imageUrl);
        }
        Item savedItem = itemRepository.save(item);
        return savedItem;
    }

    public void deleteItem(Long id) {
        itemRepository.findById(id).ifPresent(item -> {
            if (item.getImageUrl() != null) {
                try {
                    cloudinaryService.deleteFile(item.getImageUrl());
                } catch (IOException e) {
                    System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
                }
            }
            itemRepository.deleteById(id);
        });
    }

    public List<Item> getItemsBySeller(User seller) {
        return itemRepository.findBySeller(seller);
    }

    public List<Item> getActiveItemsBySeller(User seller) {
        return itemRepository.findBySellerAndStatus(seller, "出品中");
    }

    public long getItemCountBySeller(User seller) {
        return itemRepository.countBySeller(seller);
    }

    public void markItemAsSold(Long itemId) {
        itemRepository.findById(itemId).ifPresent(item -> {
            item.setStatus("売却済");
            itemRepository.save(item);
        });
    }
}