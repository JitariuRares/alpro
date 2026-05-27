package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.AuditLogDto;
import com.placute.ocrbackend.model.AuditLog;
import com.placute.ocrbackend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @PreAuthorize("hasRole('POLICE')")
    @GetMapping
    public ResponseEntity<List<AuditLogDto>> getAuditLogs(
            @RequestParam(value = "actor", required = false) String actor,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "plate", required = false) String plate,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        int normalizedLimit = normalizeLimit(limit);
        Specification<AuditLog> filters = buildFilters(
                normalizeFilter(actor),
                normalizeAction(action),
                normalizeFilter(plate),
                from,
                to
        );

        List<AuditLogDto> logs = auditLogRepository.findAll(
                        filters,
                        PageRequest.of(0, normalizedLimit, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
                ).getContent()
                .stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(logs);
    }

    private Specification<AuditLog> buildFilters(
            String actor,
            String action,
            String plate,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (actor != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("actorUsername")),
                                "%" + actor.toLowerCase() + "%"
                        )
                );
            }

            if (action != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("action"), action)
                );
            }

            if (plate != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("targetPlateNumber"), "")),
                                "%" + plate.toLowerCase() + "%"
                        )
                );
            }

            if (from != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from)
                );
            }

            if (to != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to)
                );
            }

            return predicates;
        };
    }

    private AuditLogDto toDto(AuditLog auditLog) {
        return new AuditLogDto(
                auditLog.getId(),
                auditLog.getActorUsername(),
                auditLog.getAction(),
                auditLog.getTargetPlateNumber(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeAction(String action) {
        if (action == null || action.trim().isBlank()) {
            return null;
        }
        return action.trim().toUpperCase();
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 500);
    }
}
