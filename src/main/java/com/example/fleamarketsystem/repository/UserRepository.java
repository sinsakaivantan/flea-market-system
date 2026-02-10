// src/main/java/com/example/fleamarketsystem/repository/UserRepository.java
package com.example.fleamarketsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByEmail(String email);

	Optional<User> findByName(String name);
	// キャストは CAST(... AS double precision) にして、:userId との衝突を回避
	@Query(value = """
			SELECT CAST(COALESCE(AVG(r.rating), 0) AS double precision)
			  FROM review r
			 WHERE r.seller_id   = :userId
			    OR r.reviewer_id = :userId
			""", nativeQuery = true)
	Double averageRatingForUser(@Param("userId") Long userId);
	Optional<User> findByTrust(int trust);
	Optional<User> findById(Long id);
	List<User> findByBannedTrue();
}
