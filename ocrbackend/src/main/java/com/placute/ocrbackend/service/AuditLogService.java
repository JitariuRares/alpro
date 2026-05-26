package com.placute.ocrbackend.service;

import com.placute.ocrbackend.model.AuditLog;
import com.placute.ocrbackend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void log(Authentication authentication, String action, String targetPlateNumber, String details) {
        AuditLog auditLog = new AuditLog();
        String actor = authentication != null && authentication.getName() != null
                ? authentication.getName()
                : "anonymous";

        auditLog.setActorUsername(actor);
        auditLog.setAction(action);
        auditLog.setTargetPlateNumber(targetPlateNumber);
        auditLog.setDetails(limit(details, 1000));
        auditLogRepository.save(auditLog);
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
