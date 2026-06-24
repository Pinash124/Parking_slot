package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.TransactionHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TransactionHistoryRepository extends
        JpaRepository<TransactionHistory, Long>,
        JpaSpecificationExecutor<TransactionHistory> {

    boolean existsByReferenceCodeIgnoreCase(String referenceCode);

    Optional<TransactionHistory> findByReferenceCodeIgnoreCase(String referenceCode);

    @Query("select transaction from TransactionHistory transaction "
            + "left join transaction.payment payment "
            + "order by payment.paymentTime desc")
    List<TransactionHistory> findRecent(Pageable pageable);
}
