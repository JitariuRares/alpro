package com.placute.ocrbackend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.placute.ocrbackend.model.AppUser;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.UserRole;
import com.placute.ocrbackend.repository.AppUserRepository;
import com.placute.ocrbackend.repository.AuditLogRepository;
import com.placute.ocrbackend.repository.InsuranceRepository;
import com.placute.ocrbackend.repository.LicensePlateRepository;
import com.placute.ocrbackend.repository.OcrHistoryRepository;
import com.placute.ocrbackend.repository.ParkingHistoryRepository;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import com.placute.ocrbackend.repository.VideoJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected AppUserRepository appUserRepository;

    @Autowired
    protected LicensePlateRepository licensePlateRepository;

    @Autowired
    protected InsuranceRepository insuranceRepository;

    @Autowired
    protected ParkingHistoryRepository parkingHistoryRepository;

    @Autowired
    protected OcrHistoryRepository ocrHistoryRepository;

    @Autowired
    protected VideoDetectionRepository videoDetectionRepository;

    @Autowired
    protected VideoJobRepository videoJobRepository;

    @Autowired
    protected AuditLogRepository auditLogRepository;

    @BeforeEach
    void resetState() throws Exception {
        auditLogRepository.deleteAll();
        videoDetectionRepository.deleteAll();
        videoJobRepository.deleteAll();
        parkingHistoryRepository.deleteAll();
        insuranceRepository.deleteAll();
        ocrHistoryRepository.deleteAll();
        licensePlateRepository.deleteAll();
        appUserRepository.deleteAll();

        Path uploadRoot = Path.of("target", "test-uploads");
        if (Files.exists(uploadRoot)) {
            try (var walk = Files.walk(uploadRoot)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            if (!path.equals(uploadRoot)) {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception ignored) {
                                }
                            }
                        });
            }
        }
        Files.createDirectories(uploadRoot);
    }

    protected String createUserAndGetToken(String usernamePrefix, UserRole role) throws Exception {
        String username = (usernamePrefix + "_" + UUID.randomUUID())
                .replace("-", "")
                .toLowerCase(Locale.ROOT);
        String password = "TestPass123!";

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        appUserRepository.save(user);

        String requestBody = """
                {
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        return json.get("token").asText();
    }

    protected LicensePlate createPlate(String rawPlateNumber) {
        LicensePlate plate = new LicensePlate();
        plate.setPlateNumber(rawPlateNumber.trim().toUpperCase(Locale.ROOT));
        plate.setDetectedAt(LocalDateTime.now());
        plate.setBrand("Dacia");
        plate.setModel("Logan");
        plate.setOwner("Test Owner");
        plate.setConfidence(0.95d);
        return licensePlateRepository.save(plate);
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
