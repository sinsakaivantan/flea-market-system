package com.example.fleamarketsystem.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fleamarketsystem.entity.LoginStamp;
import com.example.fleamarketsystem.entity.User;

@Repository
public interface LoginStampRepository extends JpaRepository<LoginStamp, Long> {
    boolean existsByUserAndStampDate(User user, LocalDate stampDate);
    List<LoginStamp> findByUserOrderByStampDateAsc(User user);
}
