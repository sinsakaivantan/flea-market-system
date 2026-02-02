// src/main/java/com/example/fleamarketsystem/service/AdminUserService.java
package com.example.fleamarketsystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserComplaint;
import com.example.fleamarketsystem.repository.FavoriteItemRepository;
import com.example.fleamarketsystem.repository.FollowRepository;
import com.example.fleamarketsystem.repository.UserComplaintRepository;
import com.example.fleamarketsystem.repository.UserRepository;
import com.example.fleamarketsystem.service.EmailService;

@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final UserComplaintRepository complaintRepository;
	private final FollowRepository followRepository;
	private final FavoriteItemRepository favoriteItemRepository;
	private final EmailService emailService;

	public AdminUserService(UserRepository userRepository, UserComplaintRepository complaintRepository,
			FollowRepository followRepository, FavoriteItemRepository favoriteItemRepository,
			EmailService emailService) {
		this.userRepository = userRepository;
		this.complaintRepository = complaintRepository;
		this.followRepository = followRepository;
		this.favoriteItemRepository = favoriteItemRepository;
		this.emailService = emailService;
	}

	public List<User> listAllUsers() {
		return userRepository.findAll();
	}

	public User findUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("User not found: " + id));
	}

	public Double averageRating(Long userId) {
		Double avg = userRepository.averageRatingForUser(userId);
		return (avg == null) ? 0.0 : avg;
	}

	public long complaintCount(Long userId) {
		return complaintRepository.countByReportedUserId(userId);
	}

	public List<UserComplaint> complaints(Long userId) {
		return complaintRepository.findByReportedUserIdOrderByCreatedAtDesc(userId);
	}

	public List<UserComplaint> getAllComplaintsOrderByCreatedAtDesc() {
		return complaintRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional
	public void banUser(Long targetUserId, Long adminUserId, String reason, boolean alsoDisableLogin) {
		User u = findUser(targetUserId);
		u.setBanned(true);
		u.setBanReason(reason);
		u.setBannedAt(LocalDateTime.now());
		u.setBannedByAdminId(adminUserId == null ? null : adminUserId.intValue());
		if (alsoDisableLogin)
			u.setEnabled(false);
		userRepository.save(u);
		// BANされたユーザーのフォロー・フォロワーを削除
		followRepository.deleteByFollower(u);
		followRepository.deleteByFollowing(u);
		// BANされたユーザーの商品につけられているお気に入りを全て削除
		favoriteItemRepository.deleteByItem_Seller(u);
		// BANされたユーザーにメールで通知
		String subject = "【フリマ】アカウントが停止されました";
		String body = u.getName() + " 様\n\n"
				+ "お客様のアカウントは運営により無期限アカウント停止となりました。\n\n"
				+ "【停止理由】\n" + (reason != null ? reason : "");
		emailService.sendEmail(u.getEmail(), subject, body);
	}

	@Transactional
	public void unbanUser(Long targetUserId) {
		User u = findUser(targetUserId);
		u.setBanned(false);
		u.setBanReason(null);
		u.setBannedAt(null);
		u.setBannedByAdminId(null);
		u.setEnabled(true); // 任意
		userRepository.save(u);
	}
}
