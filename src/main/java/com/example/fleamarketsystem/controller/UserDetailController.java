package com.example.fleamarketsystem.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fleamarketsystem.entity.Review;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.FollowService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.ReviewService;
import com.example.fleamarketsystem.service.UserService;

@Controller
public class UserDetailController {

	private final UserService userService;
	private final ItemService itemService;
	private final ReviewService reviewService;
	private final FollowService followService;

	public UserDetailController(UserService userService, ItemService itemService, ReviewService reviewService,
			FollowService followService) {
		this.userService = userService;
		this.itemService = itemService;
		this.reviewService = reviewService;
		this.followService = followService;
	}

	private boolean isAdmin(UserDetails userDetails) {
		return userDetails != null && userDetails.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
	}

	@GetMapping("/users/{id}")
	public String userDetail(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {
		User targetUser = userService.getUserById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (targetUser.isBanned() && !isAdmin(userDetails)) {
			redirectAttributes.addFlashAttribute("errorMessage", "このページは表示できません。");
			return "redirect:/items";
		}

		long followingCount = followService.getFollowingCount(targetUser);
		long followerCount = followService.getFollowerCount(targetUser);
		long itemCount = itemService.getItemCountBySeller(targetUser);

		List<Review> sellerReviews = reviewService.getReviewsBySeller(targetUser);
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

		model.addAttribute("user", targetUser);
		model.addAttribute("followingCount", followingCount);
		model.addAttribute("followerCount", followerCount);
		model.addAttribute("itemCount", itemCount);
		model.addAttribute("activeItems", itemService.getActiveItemsBySeller(targetUser));
		model.addAttribute("averageRating", roundedRating);
		model.addAttribute("averageRatingFormatted", String.format("%.1f", roundedRating));
		model.addAttribute("reviewCount", reviewCount);
		model.addAttribute("starClasses", starClasses);

		if (userDetails != null) {
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("Current user not found"));
			model.addAttribute("isFollowing", followService.isFollowing(currentUser, targetUser));
			model.addAttribute("isOwnPage", currentUser.getId().equals(targetUser.getId()));
		}

		return "user_detail";
	}

	@GetMapping("/users/{id}/followlist")
	public String userFollowList(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {
		User targetUser = userService.getUserById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (targetUser.isBanned() && !isAdmin(userDetails)) {
			redirectAttributes.addFlashAttribute("errorMessage", "このページは表示できません。");
			return "redirect:/items";
		}

		java.util.List<User> followingUsers = followService.getFollowingUsers(targetUser);
		java.util.Map<Long, Boolean> followingStatusMap = new java.util.HashMap<>();

		if (userDetails != null) {
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("Current user not found"));
			for (User user : followingUsers) {
				followingStatusMap.put(user.getId(), followService.isFollowing(currentUser, user));
			}
			model.addAttribute("currentUser", currentUser);
		}

		model.addAttribute("targetUser", targetUser);
		model.addAttribute("followingUsers", followingUsers);
		model.addAttribute("followingStatusMap", followingStatusMap);
		return "user_follow_list";
	}

	@GetMapping("/users/{id}/followers")
	public String userFollowersList(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model,
			RedirectAttributes redirectAttributes) {
		User targetUser = userService.getUserById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (targetUser.isBanned() && !isAdmin(userDetails)) {
			redirectAttributes.addFlashAttribute("errorMessage", "このページは表示できません。");
			return "redirect:/items";
		}

		java.util.List<User> followerUsers = followService.getFollowerUsers(targetUser);
		java.util.Map<Long, Boolean> followingStatusMap = new java.util.HashMap<>();

		if (userDetails != null) {
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("Current user not found"));
			for (User user : followerUsers) {
				followingStatusMap.put(user.getId(), followService.isFollowing(currentUser, user));
			}
			model.addAttribute("currentUser", currentUser);
		}

		model.addAttribute("targetUser", targetUser);
		model.addAttribute("followerUsers", followerUsers);
		model.addAttribute("followingStatusMap", followingStatusMap);
		return "user_followers_list";
	}
}
