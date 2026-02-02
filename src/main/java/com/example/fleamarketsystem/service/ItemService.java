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
import java.util.ArrayList;
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

    /** 商品一覧用：BAN・無効アカウントの出品者の商品は除外 */
    public Page<Item> searchItems(String keyword, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String status = "出品中";
        if (keyword != null && !keyword.isEmpty() && categoryId != null) {
            return itemRepository.findByNameContainingIgnoreCaseAndCategoryIdAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(keyword, categoryId, status, pageable);
        } else if (keyword != null && !keyword.isEmpty()) {
            return itemRepository.findByNameContainingIgnoreCaseAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(keyword, status, pageable);
        } else if (categoryId != null) {
            return itemRepository.findByCategoryIdAndStatusAndSeller_BannedFalseAndSeller_EnabledTrue(categoryId, status, pageable);
        } else {
            return itemRepository.findByStatusAndSeller_BannedFalseAndSeller_EnabledTrue(status, pageable);
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

    public Item saveItemWithoutImages(Item item) {
        return itemRepository.save(item);
    }

    public Item saveItem(Item item, List<MultipartFile> imageFiles) throws IOException {
        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile file : imageFiles) {
                if (file != null && !file.isEmpty()) {
                    String imageUrl = cloudinaryService.uploadFile(file);
                    if (imageUrl != null) {
                        uploadedUrls.add(imageUrl);
                    }
                }
            }
            if (!uploadedUrls.isEmpty()) {
                item.setImageUrls(uploadedUrls);
                item.setImageUrl(uploadedUrls.get(0));
            }
        }
        Item savedItem = itemRepository.save(item);
        return savedItem;
    }

    /** 編集時：既存画像URLの並び＋新規アップロードをマージして保存 */
    public Item saveItemWithExistingAndNew(Item item, List<String> existingImageUrls, List<MultipartFile> newImageFiles) throws IOException {
        List<String> finalUrls = new ArrayList<>();
        if (existingImageUrls != null) {
            finalUrls.addAll(existingImageUrls);
        }
        if (newImageFiles != null) {
            for (MultipartFile file : newImageFiles) {
                if (file != null && !file.isEmpty()) {
                    String imageUrl = cloudinaryService.uploadFile(file);
                    if (imageUrl != null) {
                        finalUrls.add(imageUrl);
                    }
                }
            }
        }
        if (finalUrls.size() > 10) {
            finalUrls = finalUrls.subList(0, 10);
        }
        item.setImageUrls(finalUrls);
        item.setImageUrl(finalUrls.isEmpty() ? null : finalUrls.get(0));
        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        itemRepository.findById(id).ifPresent(item -> {
            // 複数画像を削除
            if (item.getImageUrls() != null) {
                for (String imageUrl : item.getImageUrls()) {
                    try {
                        cloudinaryService.deleteFile(imageUrl);
                    } catch (IOException e) {
                        System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
                    }
                }
            }
            // 旧形式のimageUrlも削除
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