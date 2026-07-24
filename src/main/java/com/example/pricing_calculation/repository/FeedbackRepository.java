package com.example.pricing_calculation.repository;

import com.example.pricing_calculation.domain.Feedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Feedback> findByFeedbackTypeIgnoreCaseOrderByCreatedAtDesc(String feedbackType);

    List<Feedback> findByUserIdAndFeedbackTypeIgnoreCaseOrderByCreatedAtDesc(
            Long userId,
            String feedbackType);

    long deleteBySessionId(Long sessionId);
}
