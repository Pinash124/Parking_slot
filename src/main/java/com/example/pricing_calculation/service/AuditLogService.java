package com.example.pricing_calculation.service;

import com.example.pricing_calculation.domain.AuditLog;
import com.example.pricing_calculation.domain.UserAccount;
import com.example.pricing_calculation.dto.AuditLogResponse;
import com.example.pricing_calculation.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UserAccount user, String action, String entityName, Long entityId) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return auditLogRepository.findAll(
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(AuditLogResponse::from)
                .toList();
    }
}
