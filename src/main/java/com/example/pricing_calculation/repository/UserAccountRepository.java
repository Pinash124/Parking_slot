package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserAccount> findAllByOrderByIdAsc();

    long countByRoleIgnoreCaseAndStatusIgnoreCase(String role, String status);
}
