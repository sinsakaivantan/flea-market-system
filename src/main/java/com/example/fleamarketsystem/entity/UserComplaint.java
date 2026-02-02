// src/main/java/com/example/fleamarketsystem/entity/UserComplaint.java
package com.example.fleamarketsystem.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_complaint")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserComplaint {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "reported_user_id", nullable = false)
	private User reportedUserId;
	
	@ManyToOne
	@JoinColumn(name = "reporter_user_id", nullable = false)
	private User reporterUserId;

	@Column(nullable = false)
	private String reason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
	
	@Column(nullable = true)
	private Integer sikibetu = 0;
}
