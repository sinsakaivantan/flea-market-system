package com.example.fleamarketsystem.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.FollowService;
import com.example.fleamarketsystem.service.UserService;

@Controller
public class FollowController {
	private final FollowService followService;
	private final UserService userService;

	public FollowController(FollowService followService, UserService userService) {
		this.followService = followService;
		this.userService = userService;
	}

	@PostMapping("/follow/{userId}")
	public String follow(@PathVariable Long userId, @RequestParam(value = "itemId", required = false) Long itemId,
			@RequestParam(value = "returnTo", required = false, defaultValue = "items") String returnTo,
			@AuthenticationPrincipal UserDetails userDetails) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		User targetUser = userService.getUserById(userId)
				.orElseThrow(() -> new RuntimeException("Target user not found"));

		if (followService.isFollowing(currentUser, targetUser)) {
			followService.unfollow(currentUser, targetUser);
		} else {
			followService.follow(currentUser, targetUser);
		}
		if (itemId != null) {
			return "redirect:/items/" + itemId;
		}
		if ("items".equals(returnTo)) {
			return "redirect:/items";
		}
		if ("user".equals(returnTo)) {
			return "redirect:/users/" + userId;
		}
		if ("followlist".equals(returnTo)) {
			return "redirect:/followlist";
		}
		if ("followers".equals(returnTo)) {
			return "redirect:/followers";
		}
		if (returnTo != null && returnTo.startsWith("user-followlist-")) {
			String targetUserId = returnTo.substring("user-followlist-".length());
			return "redirect:/users/" + targetUserId + "/followlist";
		}
		if (returnTo != null && returnTo.startsWith("user-followers-")) {
			String targetUserId = returnTo.substring("user-followers-".length());
			return "redirect:/users/" + targetUserId + "/followers";
		}
		return "redirect:/users/" + userId;
	}

	@PostMapping("/follow/{userId}/unfollow")
	public String unfollow(@PathVariable Long userId, @RequestParam(value = "itemId", required = false) Long itemId,
			@RequestParam(value = "returnTo", required = false, defaultValue = "items") String returnTo,
			@AuthenticationPrincipal UserDetails userDetails) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		User targetUser = userService.getUserById(userId)
				.orElseThrow(() -> new RuntimeException("Target user not found"));

		followService.unfollow(currentUser, targetUser);
		if (itemId != null) {
			return "redirect:/items/" + itemId;
		}
		if ("items".equals(returnTo)) {
			return "redirect:/items";
		}
		if ("user".equals(returnTo)) {
			return "redirect:/users/" + userId;
		}
		if ("followlist".equals(returnTo)) {
			return "redirect:/followlist";
		}
		if ("followers".equals(returnTo)) {
			return "redirect:/followers";
		}
		if (returnTo != null && returnTo.startsWith("user-followlist-")) {
			String targetUserId = returnTo.substring("user-followlist-".length());
			return "redirect:/users/" + targetUserId + "/followlist";
		}
		if (returnTo != null && returnTo.startsWith("user-followers-")) {
			String targetUserId = returnTo.substring("user-followers-".length());
			return "redirect:/users/" + targetUserId + "/followers";
		}
		return "redirect:/users/" + userId;
	}

	@GetMapping("/followlist")
	public String followList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		java.util.List<User> followingUsers = followService.getFollowingUsers(currentUser);
		java.util.Map<Long, Boolean> followingStatusMap = new java.util.HashMap<>();
		for (User user : followingUsers) {
			followingStatusMap.put(user.getId(), followService.isFollowing(currentUser, user));
		}

		model.addAttribute("followingUsers", followingUsers);
		model.addAttribute("followingStatusMap", followingStatusMap);
		model.addAttribute("currentUser", currentUser);
		return "follow_list";
	}

	@GetMapping("/followers")
	public String followersList(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		java.util.List<User> followerUsers = followService.getFollowerUsers(currentUser);
		java.util.Map<Long, Boolean> followingStatusMap = new java.util.HashMap<>();
		for (User user : followerUsers) {
			followingStatusMap.put(user.getId(), followService.isFollowing(currentUser, user));
		}

		model.addAttribute("followerUsers", followerUsers);
		model.addAttribute("followingStatusMap", followingStatusMap);
		model.addAttribute("currentUser", currentUser);
		return "followers_list";
	}
}
