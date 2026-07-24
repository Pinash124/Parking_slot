package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Payment;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByStatusIgnoreCase(String status);

    List<Payment> findBySessionIdOrderByPaymentTimeDesc(Long sessionId);

    Optional<Payment> findFirstBySessionIdOrderByPaymentTimeDesc(Long sessionId);

    Optional<Payment> findFirstBySessionIdAndStatusInOrderByPaymentTimeDesc(
            Long sessionId,
            Collection<String> statuses);

    @Query("""
            select coalesce(sum(payment.amount), 0)
            from Payment payment
            where upper(payment.status) in ('COMPLETED', 'SUCCESS')
              and payment.paymentTime >= :from
              and payment.paymentTime < :to
            """)
    java.math.BigDecimal sumCompletedAmountBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

}
