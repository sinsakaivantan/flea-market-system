package com.example.fleamarketsystem.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.Category;
import com.example.fleamarketsystem.entity.Item;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.CategoryService;
import com.example.fleamarketsystem.service.ChatService;
import com.example.fleamarketsystem.service.FavoriteService;
import com.example.fleamarketsystem.service.FollowService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.ReviewService;
import com.example.fleamarketsystem.service.UserService;

@Controller
@RequestMapping("/items")
public class ItemController {

	private static final java.math.BigDecimal MIN_PRICE = new java.math.BigDecimal("50");
	private static final java.math.BigDecimal MAX_PRICE = new java.math.BigDecimal("99999999.99");

	private final ItemService itemService;
	private final CategoryService categoryService;
	private final UserService userService;
	private final ChatService chatService;
	private final FavoriteService favoriteService;
	private final ReviewService reviewService;
	private final FollowService followService;

	public ItemController(ItemService itemService, CategoryService categoryService, UserService userService,
			ChatService chatService, FavoriteService favoriteService, ReviewService reviewService,
			FollowService followService) {
		this.itemService = itemService;
		this.categoryService = categoryService;
		this.userService = userService;
		this.chatService = chatService;
		this.favoriteService = favoriteService;
		this.reviewService = reviewService;
		this.followService = followService;
	}

	@GetMapping
	public String listItems(
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {
		Page<Item> items = itemService.searchItems(keyword, categoryId, page, size);
		
		if (items.getTotalPages() > 0 && page >= items.getTotalPages()) {
			return "redirect:/items?page=0" + 
					(keyword != null ? "&keyword=" + keyword : "") +
					(categoryId != null ? "&categoryId=" + categoryId : "");
		}
		
		List<Category> categories = categoryService.getAllCategories();

		if (userDetails != null) {
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));
			model.addAttribute("currentUser", currentUser);
		}

		model.addAttribute("items", items);
		model.addAttribute("categories", categories);
		return "item_list";
	}

	@GetMapping("/{id}")
	public String showItemDetail(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails,
			Model model) {
		Optional<Item> item = itemService.getItemById(id);
		if (item.isEmpty()) {
			return "redirect:/items"; // Item not found
		}
		Item currentItem = item.get();
		model.addAttribute("item", currentItem);

		// Add seller's average rating（マイページ・ユーザー詳細と同じ0.5刻みで表示）
		reviewService.getAverageRatingForSeller(currentItem.getSeller())
				.ifPresent(avg -> {
					double rounded = Math.round(avg * 2.0) / 2.0;
					model.addAttribute("sellerAverageRating", String.format("%.1f", rounded));
				});

		// Get seller's active items (excluding current item, max 4)
		List<Item> sellerActiveItems = itemService.getActiveItemsBySeller(currentItem.getSeller())
				.stream()
				.filter(i -> !i.getId().equals(id))
				.limit(4)
				.toList();
		model.addAttribute("sellerActiveItems", sellerActiveItems);
		model.addAttribute("favoriteCount", favoriteService.getFavoriteCountByItem(currentItem));

		if (userDetails != null) {
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));
			model.addAttribute("isFavorited", favoriteService.isFavorited(currentUser, id));
			if (!currentItem.getSeller().getId().equals(currentUser.getId())) {
				model.addAttribute("isFollowing",
						followService.isFollowing(currentUser, currentItem.getSeller()));
			}
		}
		return "item_detail";
	}

	@GetMapping("/new")
	public String showAddItemForm(Model model) {
		model.addAttribute("item", new Item());
		model.addAttribute("categories", categoryService.getAllCategories());
		return "item_form";
	}

	@PostMapping
	public String addItem(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("name") String name,
			@RequestParam("description") String description,
			@RequestParam("price") BigDecimal price,
			@RequestParam("categoryId") Long categoryId,
			@RequestParam(value = "images", required = false) List<MultipartFile> imageFiles,
			RedirectAttributes redirectAttributes) {

		if (price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "価格は50円以上、99,999,999.99円以下にしてください。");
			return "redirect:/items/new";
		}

		// 画像数のチェック（最大10枚）
		if (imageFiles != null) {
			long imageCount = imageFiles.stream().filter(file -> file != null && !file.isEmpty()).count();
			if (imageCount > 10) {
				redirectAttributes.addFlashAttribute("errorMessage", "画像は最大10枚まで選択できます。");
				return "redirect:/items/new";
			}
		}

		User seller = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Seller not found"));
		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));

		Item item = new Item();
		item.setSeller(seller);
		item.setName(name);
		item.setDescription(description);
		item.setPrice(price);
		item.setCategory(category);

		try {
			itemService.saveItem(item, imageFiles);
			redirectAttributes.addFlashAttribute("successMessage", "商品を出品しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/new";
		}

		return "redirect:/items";
	}

	@GetMapping("/{id}/edit")
	public String showEditItemForm(@PathVariable("id") Long id, Model model) {
		Optional<Item> item = itemService.getItemById(id);
		if (item.isEmpty()) {
			return "redirect:/items";
		}
		Item it = item.get();
		model.addAttribute("item", it);
		model.addAttribute("categories", categoryService.getAllCategories());
		List<String> editImageUrls = (it.getImageUrls() != null && !it.getImageUrls().isEmpty())
			? it.getImageUrls()
			: (it.getImageUrl() != null ? java.util.Collections.singletonList(it.getImageUrl()) : java.util.Collections.emptyList());
		model.addAttribute("editImageUrls", editImageUrls);
		return "item_form";
	}

	@PostMapping("/{id}") // Using POST for simplicity, can be PUT with HiddenHttpMethodFilter
	public String updateItem(
			@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("name") String name,
			@RequestParam("description") String description,
			@RequestParam("price") BigDecimal price,
			@RequestParam("categoryId") Long categoryId,
			@RequestParam(value = "existingImageUrls", required = false) List<String> existingImageUrls,
			@RequestParam(value = "images", required = false) List<MultipartFile> imageFiles,
			RedirectAttributes redirectAttributes) {

		if (price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "価格は50円以上、99,999,999.99円以下にしてください。");
			return "redirect:/items/{id}/edit";
		}

		int existingCount = (existingImageUrls != null) ? existingImageUrls.size() : 0;
		long newCount = (imageFiles != null) ? imageFiles.stream().filter(file -> file != null && !file.isEmpty()).count() : 0;
		if (existingCount + newCount > 10) {
			redirectAttributes.addFlashAttribute("errorMessage", "画像は最大10枚まで選択できます。");
			return "redirect:/items/{id}/edit";
		}

		Item existingItem = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!existingItem.getSeller().getId().equals(currentUser.getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は編集できません。");
			return "redirect:/items";
		}

		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));

		existingItem.setName(name);
		existingItem.setDescription(description);
		existingItem.setPrice(price);
		existingItem.setCategory(category);

		try {
			itemService.saveItemWithExistingAndNew(existingItem, existingImageUrls, imageFiles);
			redirectAttributes.addFlashAttribute("successMessage", "商品を更新しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/{id}/edit";
		}

		return "redirect:/items/{id}";
	}

	@PostMapping("/{id}/delete")
	public String deleteItem(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		Item itemToDelete = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!itemToDelete.getSeller().getId().equals(currentUser.getId())) {
			// Only seller can delete their item
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は削除できません。");
			return "redirect:/items";
		}

		itemService.deleteItem(id);
		redirectAttributes.addFlashAttribute("successMessage", "商品を削除しました。");
		return "redirect:/items";
	}

	@PostMapping("/{id}/favorite")
	public String addFavorite(@PathVariable("id") Long itemId, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		try {
			favoriteService.addFavorite(currentUser, itemId);
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/items/{id}";
	}

	@PostMapping("/{id}/unfavorite")
	public String removeFavorite(@PathVariable("id") Long itemId, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		try {
			favoriteService.removeFavorite(currentUser, itemId);
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/items/{id}";
	}
}