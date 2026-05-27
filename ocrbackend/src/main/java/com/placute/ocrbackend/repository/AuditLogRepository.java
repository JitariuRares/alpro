package com.placute.ocrbackend.repository;

import com.placute.ocrbackend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findTop10ByTargetPlateNumberOrderByCreatedAtDescIdDesc(String targetPlateNumber);
    List<AuditLog> findTop6ByOrderByCreatedAtDescIdDesc();
}
