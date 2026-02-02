package com.example.fleamarketsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.Follow;
import com.example.fleamarketsystem.entity.User;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
	Optional<Follow> findByFollowerAndFollowing(User follower, User following);

	long countByFollower(User follower);

	long countByFollowing(User following);

	boolean existsByFollowerAndFollowing(User follower, User following);

	java.util.List<Follow> findByFollower(User follower);

	java.util.List<Follow> findByFollowing(User following);

	void deleteByFollower(User follower);

	void deleteByFollowing(User following);
}
