package com.example.fleamarketsystem.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.fleamarketsystem.entity.Ban;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.BanRepository;
import com.example.fleamarketsystem.repository.ItemRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryService categoryService;
    private final CloudinaryService cloudinaryService;
    private final BanRepository banRepository;

    public ItemService(ItemRepository itemRepository, CategoryService categoryService, CloudinaryService cloudinaryService,BanRepository banRepository) {
        this.itemRepository = itemRepository;
        this.categoryService = categoryService;
        this.cloudinaryService = cloudinaryService;
        this.banRepository = banRepository;
    }

    /** 商品一覧用：BAN・無効アカウントの出品者の商品は除外 */
    public Page<Item> searchItems(String keyword, Long categoryId, int page, int size) {
    	Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String status = "出品中";
        
        // 1. 現在BAN期間中のレコードを全て取得
        List<Ban> activeBans = banRepository.findByEndAfter(LocalDateTime.now());

        // 2. BAN中のユーザーIDリストを作成
        List<Long> bannedUserIds = activeBans.stream()
                .map(ban -> ban.getUserId().getId())
                .distinct()
                .collect(Collectors.toList());

        // 3. 検索キーワードの調整
        boolean hasKeyword = (keyword != null && !keyword.isEmpty());

        // 4. BANユーザーがいるかどうかで呼び出すメソッドを変える
        // （※NotInに空リストを渡すとエラーになるDBがあるため分岐する）
        
        if (bannedUserIds.isEmpty()) {
            // --- BANされている人が誰もいない場合（既存の検索） ---
            if (hasKeyword && categoryId != null) {
                return itemRepository.findByNameContainingIgnoreCaseAndCategoryIdAndStatusAndSeller_EnabledTrue(keyword, categoryId, status, pageable);
            } else if (hasKeyword) {
                return itemRepository.findByNameContainingIgnoreCaseAndStatusAndSeller_EnabledTrue(keyword, status, pageable);
            } else if (categoryId != null) {
                return itemRepository.findByCategoryIdAndStatusAndSeller_EnabledTrue(categoryId, status, pageable);
            } else {
                return itemRepository.findByStatusAndSeller_EnabledTrue(status, pageable);
            }
            
        } else {
            // --- BANされている人を除外して検索（NotInを使う） ---
            if (hasKeyword && categoryId != null) {
                return itemRepository.findByNameContainingIgnoreCaseAndCategoryIdAndStatusAndSeller_EnabledTrueAndSeller_IdNotIn(
                        keyword, categoryId, status, bannedUserIds, pageable);
            } else if (hasKeyword) {
                return itemRepository.findByNameContainingIgnoreCaseAndStatusAndSeller_EnabledTrueAndSeller_IdNotIn(
                        keyword, status, bannedUserIds, pageable);
            } else if (categoryId != null) {
                return itemRepository.findByCategoryIdAndStatusAndSeller_EnabledTrueAndSeller_IdNotIn(
                        categoryId, status, bannedUserIds, pageable);
            } else {
                return itemRepository.findByStatusAndSeller_EnabledTrueAndSeller_IdNotIn(
                        status, bannedUserIds, pageable);
            }
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