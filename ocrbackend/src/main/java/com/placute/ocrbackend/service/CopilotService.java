package com.placute.ocrbackend.service;

import com.placute.ocrbackend.dto.CopilotChatResponse;
import com.placute.ocrbackend.dto.DashboardStatsDto;
import com.placute.ocrbackend.dto.ParkingSessionDto;
import com.placute.ocrbackend.dto.PoliceLookupDto;
import com.placute.ocrbackend.integration.OpenAICopilotClient;
import com.placute.ocrbackend.model.AuditLog;
import com.placute.ocrbackend.model.DetectionReviewStatus;
import com.placute.ocrbackend.model.Insurance;
import com.placute.ocrbackend.model.UserRole;
import com.placute.ocrbackend.repository.AppUserRepository;
import com.placute.ocrbackend.repository.AuditLogRepository;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CopilotService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private OpenAICopilotClient openAICopilotClient;

    @Autowired
    private PoliceLookupService policeLookupService;

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private OcrHistoryRepository ocrHistoryRepository;

    @Autowired
    private VideoDetectionRepository videoDetectionRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private RomanianPlateValidator plateValidator;

    public CopilotChatResponse chat(String rawMessage, Authentication authentication) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isBlank()) {
            return helpResponse(false, "Scrie ce vrei sa cauti: ex. \"dosar B123ABC\" sau \"detectii de revizuit\".", null);
        }

        var currentUser = resolveCurrentUser(authentication);
        Set<Intent> allowedIntents = allowedIntentsFor(currentUser.getRole());

        Decision decision = buildDecision(message, currentUser.getRole(), allowedIntents);

        CopilotChatResponse response;
        if (!allowedIntents.contains(decision.intent())) {
            response = forbiddenIntentResponse(currentUser.getRole(), decision.intent(), decision.usedAiPlanner());
            auditLogService.log(
                    authentication,
                    "COPILOT_DENIED",
                    decision.plateNumber(),
                    "intent=%s, role=%s, msg=%s".formatted(decision.intent().name(), currentUser.getRole().name(), truncate(message, 180))
            );
            return response;
        }

        try {
            response = executeDecision(decision, message);
            auditLogService.log(
                    authentication,
                    "COPILOT_QUERY",
                    decision.plateNumber(),
                    "intent=%s, tool=%s, aiPlanner=%s, msg=%s".formatted(
                            decision.intent().name(),
                            response.getTool(),
                            decision.usedAiPlanner(),
                            truncate(message, 180)
                    )
            );
            return response;
        } catch (RuntimeException ex) {
            CopilotChatResponse failure = helpResponse(
                    decision.usedAiPlanner(),
                    "Nu am putut finaliza cererea: " + safeText(ex.getMessage(), "a aparut o eroare."),
                    null
            );
            auditLogService.log(
                    authentication,
                    "COPILOT_ERROR",
                    decision.plateNumber(),
                    "intent=%s, msg=%s, error=%s".formatted(
                            decision.intent().name(),
                            truncate(message, 120),
                            truncate(ex.getMessage(), 280)
                    )
            );
            return failure;
        }
    }

    private Decision buildDecision(String message, UserRole userRole, Set<Intent> allowedIntents) {
        Set<String> allowedForPrompt = allowedIntents.stream()
                .map(Intent::name)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        Optional<OpenAICopilotClient.CopilotPlan> aiPlan = openAICopilotClient
                .planIntent(message, userRole, allowedForPrompt);

        if (aiPlan.isPresent()) {
            Decision fromAi = toDecision(aiPlan.get(), message, true);
            if (fromAi.intent() != Intent.HELP) {
                return fromAi;
            }
        }

        return fallbackDecision(message);
    }

    private Decision toDecision(OpenAICopilotClient.CopilotPlan plan, String message, boolean usedAiPlanner) {
        Intent intent = parseIntent(plan.intent()).orElse(Intent.HELP);
        String plate = normalizePlate(plan.plateNumber());
        if (plate == null) {
            plate = extractPlate(message);
        }

        return new Decision(
                intent,
                plate,
                normalizeReviewStatus(plan.reviewStatus()),
                normalizeModule(plan.targetModule()),
                normalizeNullable(plan.actor()),
                usedAiPlanner
        );
    }

    private Decision fallbackDecision(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        String plate = extractPlate(message);

        if (containsAny(normalized, "review", "reviz", "detect")) {
            return new Decision(Intent.REVIEW_QUEUE, plate, "DE_REVIEW", "detectii", null, false);
        }

        if (containsAny(normalized, "audit", "log", "actor")) {
            String actor = extractActor(normalized);
            return new Decision(Intent.AUDIT_RECENT, plate, null, "audit", actor, false);
        }

        if (containsAny(normalized, "asigur", "polita")) {
            return new Decision(Intent.INSURANCE_LOOKUP, plate, null, "asigurari", null, false);
        }

        if (containsAny(normalized, "parcare", "parking", "intrare", "iesire")) {
            return new Decision(Intent.PARKING_LOOKUP, plate, null, "parcare", null, false);
        }

        if (containsAny(normalized, "stat", "dashboard", "overview", "rezumat")) {
            return new Decision(Intent.DASHBOARD_STATS, plate, null, "dashboard", null, false);
        }

        if (containsAny(normalized, "du-ma", "navig", "deschide", "mergi")) {
            return new Decision(Intent.NAVIGATE, plate, null, detectModuleFromText(normalized), null, false);
        }

        if (plate != null || containsAny(normalized, "plac", "masina", "vehicul", "dosar")) {
            return new Decision(Intent.POLICE_LOOKUP, plate, null, "vehicule", null, false);
        }

        return new Decision(Intent.HELP, null, null, null, null, false);
    }

    private CopilotChatResponse executeDecision(Decision decision, String originalMessage) {
        return switch (decision.intent()) {
            case POLICE_LOOKUP -> lookupPolice(decision);
            case PARKING_LOOKUP -> lookupParking(decision);
            case INSURANCE_LOOKUP -> lookupInsurance(decision);
            case REVIEW_QUEUE -> lookupReview(decision);
            case DASHBOARD_STATS -> lookupDashboard(decision);
            case AUDIT_RECENT -> lookupAudit(decision);
            case NAVIGATE -> navigate(decision);
            case HELP -> helpResponse(decision.usedAiPlanner(), "Te pot ajuta cu cautari de placute, parking, asigurari, review, audit si dashboard.", originalMessage);
        };
    }

    private CopilotChatResponse lookupPolice(Decision decision) {
        String plate = requirePlate(decision.plateNumber(), "Spune-mi si placuta. Exemplu: \"dosar B123ABC\".");
        PoliceLookupDto result = policeLookupService.lookupByPlateNumber(plate);

        int insuranceCount = result.getInsurances() != null ? result.getInsurances().size() : 0;
        int parkingCount = result.getParkingHistory() != null ? result.getParkingHistory().size() : 0;
        int ocrCount = result.getRecentOcrDetections() != null ? result.getRecentOcrDetections().size() : 0;
        int videoCount = result.getRecentVideoDetections() != null ? result.getRecentVideoDetections().size() : 0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("plateNumber", result.getPlateNumber());
        data.put("vehicleFound", result.getPlate() != null);
        data.put("insuranceCount", insuranceCount);
        data.put("parkingSessionsCount", parkingCount);
        data.put("ocrDetectionsCount", ocrCount);
        data.put("videoDetectionsCount", videoCount);

        if (result.getPlate() != null) {
            Map<String, Object> vehicle = new LinkedHashMap<>();
            vehicle.put("brand", result.getPlate().getBrand());
            vehicle.put("model", result.getPlate().getModel());
            vehicle.put("owner", result.getPlate().getOwner());
            vehicle.put("confidence", result.getPlate().getConfidence());
            vehicle.put("detectedAt", result.getPlate().getDetectedAt());
            data.put("vehicle", vehicle);
        }

        String answer = result.getPlate() == null
                ? "Am cautat " + plate + ". Nu exista inregistrare vehicul completa, dar poti verifica istoricul si auditul."
                : "Am gasit dosarul pentru " + plate + ". Asigurari: %d, parcari: %d, detectii foto: %d, detectii video: %d."
                .formatted(insuranceCount, parkingCount, ocrCount, videoCount);

        return new CopilotChatResponse(
                answer,
                Intent.POLICE_LOOKUP.name(),
                "police_lookup",
                "/vehicule?tab=cautare&plate=" + plate,
                data,
                List.of(
                        "Deschide dosarul complet pentru " + plate,
                        "Arata doar sesiunile de parcare pentru " + plate,
                        "Arata review detectii neconfirmate"
                ),
                decision.usedAiPlanner()
        );
    }

    private CopilotChatResponse lookupParking(Decision decision) {
        String plate = requirePlate(decision.plateNumber(), "Pentru parking am nevoie de placuta. Exemplu: \"parcare B123ABC\".");
        List<ParkingSessionDto> sessions = parkingService.getSessionsByPlate(plate);
        long openCount = sessions.stream()
                .filter(session -> session.getStatus() != null && "OPEN".equals(session.getStatus().name()))
                .count();

        List<Map<String, Object>> recent = sessions.stream()
                .sorted(Comparator.comparing(ParkingSessionDto::getEntryTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(session -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", session.getId());
                    item.put("parkingZone", session.getParkingZone());
                    item.put("entryTime", session.getEntryTime());
                    item.put("exitTime", session.getExitTime());
                    item.put("status", session.getStatus() != null ? session.getStatus().name() : null);
                    item.put("durationMinutes", session.getDurationMinutes());
                    return item;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("plateNumber", plate);
        data.put("totalSessions", sessions.size());
        data.put("openSessions", openCount);
        data.put("recentSessions", recent);

        return new CopilotChatResponse(
                "Pentru %s am gasit %d sesiuni, dintre care %d deschise.".formatted(plate, sessions.size(), openCount),
                Intent.PARKING_LOOKUP.name(),
                "parking_lookup",
                "/parcare?tab=sesiuni&plate=" + plate,
                data,
                List.of(
                        "Deschide tab-ul de sesiuni pentru " + plate,
                        "Vezi doar intrarile deschise",
                        "Mergi la dashboard"
                ),
                decision.usedAiPlanner()
        );
    }

    private CopilotChatResponse lookupInsurance(Decision decision) {
        String plate = requirePlate(decision.plateNumber(), "Pentru asigurari am nevoie de placuta. Exemplu: \"asigurare B123ABC\".");
        List<Insurance> insurances = insuranceRepository.findByLicensePlate_PlateNumberOrderByValidToDesc(plate);

        LocalDate now = LocalDate.now();
        long active = insurances.stream()
                .filter(item -> item.getValidFrom() != null && item.getValidTo() != null)
                .filter(item -> !now.isBefore(item.getValidFrom()) && !now.isAfter(item.getValidTo()))
                .count();

        List<Map<String, Object>> top = insurances.stream()
                .limit(5)
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.getId());
                    row.put("company", item.getCompany());
                    row.put("validFrom", item.getValidFrom());
                    row.put("validTo", item.getValidTo());
                    return row;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("plateNumber", plate);
        data.put("totalPolicies", insurances.size());
        data.put("activePolicies", active);
        data.put("topPolicies", top);

        return new CopilotChatResponse(
                "Pentru %s am gasit %d polite (%d active azi).".formatted(plate, insurances.size(), active),
                Intent.INSURANCE_LOOKUP.name(),
                "insurance_lookup",
                "/asigurari?tab=cautare&plate=" + plate,
                data,
                List.of(
                        "Deschide cautarea asigurari pentru " + plate,
                        "Mergi la dashboard",
                        "Cauta un alt numar"
                ),
                decision.usedAiPlanner()
        );
    }

    private CopilotChatResponse lookupReview(Decision decision) {
        DetectionReviewStatus status = parseReviewStatus(decision.reviewStatus()).orElse(DetectionReviewStatus.DE_REVIEW);

        long photoCount = ocrHistoryRepository.countByReviewStatusOrNullForReview(status);
        long videoCount = videoDetectionRepository.countByReviewStatusOrNullForReview(status);

        List<Map<String, Object>> samplePhotos = ocrHistoryRepository
                .findByReviewStatusWithLicensePlateOrderByProcessedAtDesc(status, PageRequest.of(0, 5))
                .stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("source", "foto");
                    row.put("sourceId", item.getId());
                    row.put("plateNumber", item.getLicensePlate() != null ? item.getLicensePlate().getPlateNumber() : null);
                    row.put("confidence", item.getConfidence());
                    row.put("detectedAt", item.getProcessedAt());
                    return row;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reviewStatus", status.name());
        data.put("photoCount", photoCount);
        data.put("videoCount", videoCount);
        data.put("totalCount", photoCount + videoCount);
        data.put("sample", samplePhotos);

        return new CopilotChatResponse(
                "Review %s: %d detectii foto si %d detectii video."
                        .formatted(status.name(), photoCount, videoCount),
                Intent.REVIEW_QUEUE.name(),
                "detection_review_lookup",
                "/detectii?tab=review",
                data,
                List.of(
                        "Deschide panelul de review",
                        "Filtreaza dupa CONFIRMED",
                        "Filtreaza dupa REJECTED"
                ),
                decision.usedAiPlanner()
        );
    }

    private CopilotChatResponse lookupDashboard(Decision decision) {
        DashboardStatsDto stats = dashboardService.getDashboardStats();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalPlates", stats.getTotalPlates());
        data.put("totalInsurances", stats.getTotalInsurances());
        data.put("openParkings", stats.getOpenParkings());
        data.put("pendingDetectionReviews", stats.getPendingDetectionReviews());
        data.put("runningVideoJobs", stats.getRunningVideoJobs());
        data.put("activeInsurances", stats.getActiveInsurances());
        data.put("expiredInsurances", stats.getExpiredInsurances());

        return new CopilotChatResponse(
                "Dashboard rapid: %d vehicule, %d parcari deschise, %d detectii de revizuit."
                        .formatted(stats.getTotalPlates(), stats.getOpenParkings(), stats.getPendingDetectionReviews()),
                Intent.DASHBOARD_STATS.name(),
                "dashboard_stats",
                "/dashboard",
                data,
                List.of(
                        "Deschide dashboard",
                        "Arata detectii de revizuit",
                        "Arata sesiunile de parcare"
                ),
                decision.usedAiPlanner()
        );
    }

    private CopilotChatResponse lookupAudit(Decision decision) {
        String actorFilter = normalizeNullable(decision.actor());

        List<AuditLog> logs = auditLogRepository.findTop6ByOrderByCreatedAtDescIdDesc();
        if (actorFilter != null) {
            logs = logs.stream()
                    .filter(log -> log.getActorUsername() != null
                            && log.getActorUsername().toLowerCase(Locale.ROOT).contains(actorFilter.toLowerCase(Locale.ROOT)))
                    .toList();
        }

        List<Map<String, Object>> dataItems = logs.stream()
                .map(log -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", log.getId());
                    row.put("actor", log.getActorUsername());
                    row.put("action", log.getAction());
                    row.put("plateNumber", log.getTargetPlateNumber());
                    row.put("createdAt", log.getCreatedAt());
                    return row;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", logs.size());
        data.put("actorFilter", actorFilter);
        data.put("items", dataItems);

        String answer = actorFilter == null
                ? "Am extras ultimele evenimente de audit."
                : "Am extras auditul recent filtrat pentru actorul \"" + actorFilter + "\".";

        return new CopilotChatResponse(
                answer,
                Intent.AUDIT_RECENT.name(),
                "audit_recent",
                "/audit",
                data,
                List.of(
                        "Deschide pagina Audit",
                        "Filtreaza dupa actor",
                        "Exporta CSV din Audit"
                ),
                decision.usedAiPlanner()
        );
    }

    private CopilotChatResponse navigate(Decision decision) {
        String module = normalizeModule(decision.module());
        if (module == null) {
            module = "dashboard";
        }

        String deepLink = switch (module) {
            case "detectii" -> "/detectii";
            case "vehicule" -> decision.plateNumber() != null
                    ? "/vehicule?tab=cautare&plate=" + decision.plateNumber()
                    : "/vehicule";
            case "parcare" -> decision.plateNumber() != null
                    ? "/parcare?tab=sesiuni&plate=" + decision.plateNumber()
                    : "/parcare";
            case "asigurari" -> decision.plateNumber() != null
                    ? "/asigurari?tab=cautare&plate=" + decision.plateNumber()
                    : "/asigurari";
            case "audit" -> "/audit";
            default -> "/dashboard";
        };

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("module", module);
        data.put("plateNumber", decision.plateNumber());

        return new CopilotChatResponse(
                "Te pot duce direct in modulul \"" + module + "\".",
                Intent.NAVIGATE.name(),
                "navigate",
                deepLink,
                data,
                List.of(
                        "Mergi la dashboard",
                        "Mergi la detectii",
                        "Mergi la vehicule"
                ),
                decision.usedAiPlanner()
        );
    }

    private CopilotChatResponse helpResponse(boolean usedAiPlanner, String answer, String originalMessage) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (originalMessage != null && !originalMessage.isBlank()) {
            data.put("originalMessage", truncate(originalMessage, 180));
        }
        data.put("supportedExamples", List.of(
                "dosar B123ABC",
                "parcare B123ABC",
                "asigurare B123ABC",
                "detectii de revizuit",
                "audit recent",
                "statistici dashboard"
        ));

        return new CopilotChatResponse(
                answer,
                Intent.HELP.name(),
                "help",
                "/dashboard",
                data,
                List.of(
                        "Cauta dosar pentru o placuta",
                        "Vezi review detectii",
                        "Deschide audit"
                ),
                usedAiPlanner
        );
    }

    private CopilotChatResponse forbiddenIntentResponse(UserRole role, Intent intent, boolean usedAiPlanner) {
        return new CopilotChatResponse(
                "Pentru rolul %s nu ai acces la comanda %s.".formatted(role.name(), intent.name()),
                Intent.HELP.name(),
                "forbidden_intent",
                "/dashboard",
                Map.of("requestedIntent", intent.name(), "role", role.name()),
                roleSuggestions(role),
                usedAiPlanner
        );
    }

    private Set<Intent> allowedIntentsFor(UserRole role) {
        return switch (role) {
            case POLICE -> Set.of(
                    Intent.POLICE_LOOKUP,
                    Intent.PARKING_LOOKUP,
                    Intent.INSURANCE_LOOKUP,
                    Intent.REVIEW_QUEUE,
                    Intent.DASHBOARD_STATS,
                    Intent.AUDIT_RECENT,
                    Intent.NAVIGATE,
                    Intent.HELP
            );
            case PARKING -> Set.of(
                    Intent.PARKING_LOOKUP,
                    Intent.DASHBOARD_STATS,
                    Intent.NAVIGATE,
                    Intent.HELP
            );
            case INSURANCE -> Set.of(
                    Intent.INSURANCE_LOOKUP,
                    Intent.DASHBOARD_STATS,
                    Intent.NAVIGATE,
                    Intent.HELP
            );
        };
    }

    private List<String> roleSuggestions(UserRole role) {
        return switch (role) {
            case POLICE -> List.of("dosar B123ABC", "detectii de revizuit", "audit recent");
            case PARKING -> List.of("parcare B123ABC", "sesiuni deschise", "dashboard");
            case INSURANCE -> List.of("asigurare B123ABC", "polite active", "dashboard");
        };
    }

    private String requirePlate(String plate, String messageWhenMissing) {
        if (plate == null || plate.isBlank()) {
            throw new RuntimeException(messageWhenMissing);
        }
        return plate;
    }

    private String extractPlate(String message) {
        return normalizePlate(message);
    }

    private String extractActor(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return null;
        }
        int idx = normalizedText.indexOf("actor");
        if (idx < 0) {
            return null;
        }
        String tail = normalizedText.substring(idx + "actor".length()).trim();
        if (tail.startsWith(":")) {
            tail = tail.substring(1).trim();
        }
        if (tail.isBlank()) {
            return null;
        }
        String[] pieces = tail.split("\\s+");
        return pieces.length == 0 ? null : pieces[0];
    }

    private String detectModuleFromText(String normalizedText) {
        if (normalizedText == null) {
            return null;
        }
        if (containsAny(normalizedText, "detect")) return "detectii";
        if (containsAny(normalizedText, "vehicul", "masina", "plac")) return "vehicule";
        if (containsAny(normalizedText, "parcare", "parking")) return "parcare";
        if (containsAny(normalizedText, "asigur")) return "asigurari";
        if (containsAny(normalizedText, "audit")) return "audit";
        if (containsAny(normalizedText, "dashboard", "stat")) return "dashboard";
        return null;
    }

    private Optional<Intent> parseIntent(String rawIntent) {
        if (rawIntent == null || rawIntent.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Intent.valueOf(rawIntent.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<DetectionReviewStatus> parseReviewStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(DetectionReviewStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private String normalizeReviewStatus(String rawStatus) {
        return parseReviewStatus(rawStatus).map(Enum::name).orElse(null);
    }

    private String normalizeModule(String module) {
        if (module == null || module.isBlank()) {
            return null;
        }
        String normalized = module.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "dashboard", "detectii", "vehicule", "parcare", "asigurari", "audit" -> normalized;
            default -> null;
        };
    }

    private String normalizePlate(String rawPlate) {
        return plateValidator.extractNormalizedPlate(rawPlate);
    }

    private boolean containsAny(String text, String... tokens) {
        if (text == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private com.placute.ocrbackend.model.AppUser resolveCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Utilizator neautentificat.");
        }

        String username = authentication.getName();
        return appUserRepository.findTopByUsernameIgnoreCaseOrderByIdDesc(username)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit."));
    }

    private enum Intent {
        POLICE_LOOKUP,
        PARKING_LOOKUP,
        INSURANCE_LOOKUP,
        REVIEW_QUEUE,
        DASHBOARD_STATS,
        AUDIT_RECENT,
        NAVIGATE,
        HELP
    }

    private record Decision(
            Intent intent,
            String plateNumber,
            String reviewStatus,
            String module,
            String actor,
            boolean usedAiPlanner
    ) {
    }
}
