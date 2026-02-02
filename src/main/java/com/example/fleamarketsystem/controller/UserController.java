package com.example.fleamarketsystem.controller;

import static dev.samstevens.totp.util.Utils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.Review;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.AppOrderService;
import com.example.fleamarketsystem.service.CloudinaryService;
import com.example.fleamarketsystem.service.FavoriteService;
import com.example.fleamarketsystem.service.FollowService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.ReviewService;
import com.example.fleamarketsystem.service.UserService;

import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;

@Controller
@RequestMapping("/my-page")
public class UserController {

	private final UserService userService;
	private final ItemService itemService;
	private final AppOrderService appOrderService;
	private final FavoriteService favoriteService;
	private final ReviewService reviewService;
	private final FollowService followService;
	private final CloudinaryService cloudinaryService;
	private final QrGenerator qrGenerator;

	public UserController(UserService userService, ItemService itemService, AppOrderService appOrderService,
			FavoriteService favoriteService, ReviewService reviewService, FollowService followService,
			CloudinaryService cloudinaryService, QrGenerator qrGenerator) {
		this.userService = userService;
		this.itemService = itemService;
		this.appOrderService = appOrderService;
		this.favoriteService = favoriteService;
		this.reviewService = reviewService;
		this.followService = followService;
		this.cloudinaryService = cloudinaryService;
		this.qrGenerator = qrGenerator;
	}

	@GetMapping
	public String myPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		long followingCount = followService.getFollowingCount(currentUser);
		long followerCount = followService.getFollowerCount(currentUser);
		long itemCount = itemService.getItemCountBySeller(currentUser);

		List<Review> sellerReviews = reviewService.getReviewsBySeller(currentUser);
		int reviewCount = sellerReviews.size();
		double averageRating = 0.0;
		if (reviewCount > 0) {
			averageRating = sellerReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
		}
		double roundedRating = Math.round(averageRating * 2.0) / 2.0;
		int fullStars = (int) roundedRating;
		boolean hasHalfStar = roundedRating - fullStars == 0.5;

		List<String> starClasses = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			if (i <= fullStars) {
				starClasses.add("star-full");
			} else if (hasHalfStar && i == fullStars + 1) {
				starClasses.add("star-half");
			} else {
				starClasses.add("star-empty");
			}
		}

		model.addAttribute("user", currentUser);
		model.addAttribute("followingCount", followingCount);
		model.addAttribute("followerCount", followerCount);
		model.addAttribute("itemCount", itemCount);
		model.addAttribute("activeItems", itemService.getActiveItemsBySeller(currentUser));
		model.addAttribute("averageRating", roundedRating);
		model.addAttribute("averageRatingFormatted", String.format("%.1f", roundedRating));
		model.addAttribute("reviewCount", reviewCount);
		model.addAttribute("starClasses", starClasses);
		return "my_page";
	}

	@GetMapping("/selling")
	public String mySellingItems(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("sellingItems", itemService.getItemsBySeller(currentUser));
		return "seller_items";
	}

	@GetMapping("/orders")
	public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("myOrders", appOrderService.getOrdersByBuyer(currentUser));
		return "buyer_app_orders";
	}

	@GetMapping("/sales")
	public String mySales(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("mySales", appOrderService.getOrdersBySeller(currentUser));
		return "seller_app_orders";
	}

	@GetMapping("/favorites")
	public String myFavorites(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("favoriteItems", favoriteService.getFavoriteItemsByUser(currentUser));
		return "my_favorites";
	}

	@GetMapping("/reviews")
	public String myReviews(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("reviews", reviewService.getReviewsByReviewer(currentUser));
		return "user_reviews";
	}

	@GetMapping("/settings")
	public String settings(@AuthenticationPrincipal UserDetails userDetails, Model model) throws QrGenerationException {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("user", currentUser);
		model.addAttribute("ab", currentUser.isMfaEnabled());
		String secret = currentUser.getTotpSecret();
		if(secret==null) {
			return "error3";
		}
		QrData data = new QrData.Builder()
	            .label(currentUser.getEmail())
	            .secret(secret)
	            .issuer("🐬vantansinsakai")
	            .build();

	        // QRコード画像をBase64文字列として生成（<img>タグで使用可能）
	        String qrCodeImage = getDataUriForImage(
	          qrGenerator.generate(data), 
	          qrGenerator.getImageMimeType()
	        );
	        model.addAttribute("qr", qrCodeImage);
		return "settings";
	}

	@PostMapping("/settings/update")
	public String updateSettings(@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("name") String name,
			@RequestParam(value = "image", required = false) MultipartFile imageFile,
			@RequestParam(value="shake",defaultValue = "false") Boolean aoa,
			RedirectAttributes redirectAttributes) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (name == null || name.trim().isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "名前を入力してください。");
			return "redirect:/my-page/settings";
		}

		if (name.length() > 50) {
			redirectAttributes.addFlashAttribute("errorMessage", "名前は50文字以内で入力してください。");
			return "redirect:/my-page/settings";
		}

		currentUser.setName(name.trim());
		currentUser.setMfaEnabled(aoa);
		if (imageFile != null && !imageFile.isEmpty()) {
			try {
				String oldImageUrl = currentUser.getProfileImageUrl();
				String newImageUrl = cloudinaryService.uploadFile(imageFile);
				currentUser.setProfileImageUrl(newImageUrl);

				if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
					cloudinaryService.deleteFile(oldImageUrl);
				}
			} catch (IOException e) {
				redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
				return "redirect:/my-page/settings";
			}
		}

		userService.saveUser(currentUser);

		redirectAttributes.addFlashAttribute("successMessage", "設定を更新しました！");
		return "redirect:/my-page/settings";
	}

	@PostMapping("/profile-image")
	public String uploadProfileImage(@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("image") MultipartFile imageFile, RedirectAttributes redirectAttributes) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (imageFile != null && !imageFile.isEmpty()) {
			try {
				String oldImageUrl = currentUser.getProfileImageUrl();
				String newImageUrl = cloudinaryService.uploadFile(imageFile);
				currentUser.setProfileImageUrl(newImageUrl);
				userService.saveUser(currentUser);

				if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
					cloudinaryService.deleteFile(oldImageUrl);
				}

				redirectAttributes.addFlashAttribute("successMessage", "プロフィール画像を更新しました！");
			} catch (IOException e) {
				redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			}
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "画像ファイルを選択してください。");
		}

		return "redirect:/my-page/settings";
	}
	
	@GetMapping("/stamp-card")
	public String showStampCard() {
	    return "stampcard(geminigatukurimasita)"; // templates/stamp_card.html を表示する
	}
	
	@GetMapping("/geminigame")
	public String bakaAiGemini() {
		return "bakaaigemini";
	}
	
	@GetMapping("/slot")
    public String showSlot() {
        return "slot"; // templates/slot.html
    }
	
}
