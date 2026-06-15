package com.smartparking.smartparking.repository;

import com.smartparking.smartparking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}