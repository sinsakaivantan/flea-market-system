package com.example.fleamarketsystem.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fleamarketsystem.entity.Follow;
import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.repository.FollowRepository;

@Service
public class FollowService {
	private final FollowRepository followRepository;

	public FollowService(FollowRepository followRepository) {
		this.followRepository = followRepository;
	}

	public long getFollowingCount(User user) {
		return followRepository.countByFollower(user);
	}

	public long getFollowerCount(User user) {
		return followRepository.countByFollowing(user);
	}

	public boolean isFollowing(User follower, User following) {
		return followRepository.existsByFollowerAndFollowing(follower, following);
	}

	@Transactional
	public void follow(User follower, User following) {
		if (follower.getId().equals(following.getId())) {
			throw new IllegalArgumentException("自分自身をフォローすることはできません");
		}
		if (followRepository.existsByFollowerAndFollowing(follower, following)) {
			throw new IllegalArgumentException("既にフォローしています");
		}
		Follow follow = new Follow();
		follow.setFollower(follower);
		follow.setFollowing(following);
		followRepository.save(follow);
	}

	@Transactional
	public void unfollow(User follower, User following) {
		followRepository.findByFollowerAndFollowing(follower, following)
				.ifPresent(followRepository::delete);
	}

	public List<User> getFollowingUsers(User user) {
		return followRepository.findByFollower(user).stream()
				.map(Follow::getFollowing)
				.collect(Collectors.toList());
	}

	public List<User> getFollowerUsers(User user) {
		return followRepository.findByFollowing(user).stream()
				.map(Follow::getFollower)
				.collect(Collectors.toList());
	}
}
