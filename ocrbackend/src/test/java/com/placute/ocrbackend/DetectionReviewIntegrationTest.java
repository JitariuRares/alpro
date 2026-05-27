package com.placute.ocrbackend;

import com.placute.ocrbackend.model.AuditLog;
import com.placute.ocrbackend.model.AppUser;
import com.placute.ocrbackend.model.DetectionReviewStatus;
import com.placute.ocrbackend.model.LicensePlate;
import com.placute.ocrbackend.model.OcrHistory;
import com.placute.ocrbackend.model.UserRole;
import com.placute.ocrbackend.model.VideoDetection;
import com.placute.ocrbackend.model.VideoJob;
import com.placute.ocrbackend.model.VideoJobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DetectionReviewIntegrationTest extends BaseIntegrationTest {

    @Test
    void policeCanListAndConfirmPhotoDetectionReview() throws Exception {
        LicensePlate plate = createPlate("B123REV");
        OcrHistory history = ocrHistoryRepository.save(new OcrHistory(
                plate,
                "review.jpg",
                "hash-review",
                LocalDateTime.now(),
                0.81d,
                10,
                20,
                120,
                40
        ));
        String policeToken = createUserAndGetToken("police", UserRole.POLICE);

        mockMvc.perform(get("/api/detection-review")
                        .param("status", "DE_REVIEW")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sourceType").value("foto"))
                .andExpect(jsonPath("$[0].sourceId").value(history.getId()))
                .andExpect(jsonPath("$[0].plateNumber").value("B123REV"))
                .andExpect(jsonPath("$[0].reviewStatus").value("DE_REVIEW"));

        String updateBody = """
                {
                  "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/api/detection-review/foto/{sourceId}", history.getId())
                        .header("Authorization", bearer(policeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.reviewedBy").exists());

        OcrHistory saved = ocrHistoryRepository.findById(history.getId()).orElseThrow();
        assertThat(saved.getReviewStatus()).isEqualTo(DetectionReviewStatus.CONFIRMED);
        assertThat(saved.getReviewedBy()).isNotBlank();
        assertThat(saved.getReviewedAt()).isNotNull();

        List<String> actions = auditLogRepository.findAll().stream()
                .map(AuditLog::getAction)
                .toList();
        assertThat(actions).contains("DETECTION_CONFIRMED");
    }

    @Test
    void policeCanReviewVideoDetectionsAsGroupedCase() throws Exception {
        AppUser owner = new AppUser();
        owner.setUsername("video_owner");
        owner.setPassword(passwordEncoder.encode("TestPass123!"));
        owner.setRole(UserRole.POLICE);
        owner = appUserRepository.save(owner);

        VideoJob job = new VideoJob();
        job.setUser(owner);
        job.setSourceFilename("traffic.mp4");
        job.setStoredFilename("traffic.mp4");
        job.setStoragePath("target/test-uploads/videos/traffic.mp4");
        job.setStatus(VideoJobStatus.COMPLETED);
        job.setProgressPercent(100);
        job.setCreatedAt(LocalDateTime.now().minusMinutes(3));
        job.setCompletedAt(LocalDateTime.now());
        job = videoJobRepository.save(job);

        videoDetectionRepository.save(createVideoDetection(job, "B999VID", 10, 0.71d));
        videoDetectionRepository.save(createVideoDetection(job, "B999VID", 20, 0.84d));
        videoDetectionRepository.save(createVideoDetection(job, "B999VID", 30, 0.77d));
        videoDetectionRepository.save(createVideoDetection(job, "B111ALT", 40, 0.66d));

        String policeToken = createUserAndGetToken("police", UserRole.POLICE);

        String responseBody = mockMvc.perform(get("/api/detection-review")
                        .param("status", "DE_REVIEW")
                        .header("Authorization", bearer(policeToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var items = objectMapper.readTree(responseBody);
        assertThat(items).hasSize(2);
        var grouped = items.findValues("plateNumber").stream()
                .filter(node -> "B999VID".equals(node.asText()))
                .findFirst();
        assertThat(grouped).isPresent();

        String updateBody = """
                {
                  "status": "REJECTED",
                  "plateNumber": "B999VID"
                }
                """;

        mockMvc.perform(patch("/api/detection-review/video/{sourceId}", job.getId())
                        .header("Authorization", bearer(policeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("video"))
                .andExpect(jsonPath("$.sourceId").value(job.getId()))
                .andExpect(jsonPath("$.occurrenceCount").value(3))
                .andExpect(jsonPath("$.reviewStatus").value("REJECTED"));

        List<VideoDetection> rejectedGroup = videoDetectionRepository.findByJobIdAndPlateTextWithJob(job.getId(), "B999VID");
        assertThat(rejectedGroup).hasSize(3);
        assertThat(rejectedGroup).allMatch(detection -> detection.getReviewStatus() == DetectionReviewStatus.REJECTED);

        List<VideoDetection> untouchedGroup = videoDetectionRepository.findByJobIdAndPlateTextWithJob(job.getId(), "B111ALT");
        assertThat(untouchedGroup).hasSize(1);
        assertThat(untouchedGroup.getFirst().getReviewStatus()).isEqualTo(DetectionReviewStatus.DE_REVIEW);
    }

    private VideoDetection createVideoDetection(VideoJob job, String plateText, int frameIndex, double confidence) {
        VideoDetection detection = new VideoDetection();
        detection.setJob(job);
        detection.setPlateText(plateText);
        detection.setFrameIndex(frameIndex);
        detection.setTimestampMs(frameIndex * 40L);
        detection.setTrackId(1);
        detection.setConfidence(confidence);
        detection.setBboxX(10);
        detection.setBboxY(20);
        detection.setBboxW(120);
        detection.setBboxH(40);
        return detection;
    }
}
