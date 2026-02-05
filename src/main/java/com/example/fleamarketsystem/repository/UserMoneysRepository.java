package com.example.fleamarketsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.entity.UserMoneys;

public interface UserMoneysRepository extends JpaRepository<UserMoneys, Long> {
    Optional<UserMoneys> findByUser(User user);
}