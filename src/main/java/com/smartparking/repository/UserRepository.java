package com.smartparking.repository;

import com.smartparking.model.schemas.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}