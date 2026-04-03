package com.placute.ocrbackend.service;

import com.placute.ocrbackend.dto.VideoDetectionDto;
import com.placute.ocrbackend.dto.VideoDetectionPageDto;
import com.placute.ocrbackend.dto.VideoJobDto;
import com.placute.ocrbackend.model.AppUser;
import com.placute.ocrbackend.model.VideoDetection;
import com.placute.ocrbackend.model.VideoJob;
import com.placute.ocrbackend.model.VideoJobStatus;
import com.placute.ocrbackend.repository.AppUserRepository;
import com.placute.ocrbackend.repository.VideoDetectionRepository;
import com.placute.ocrbackend.repository.VideoJobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoJobService {

    @Autowired
    private VideoJobRepository videoJobRepository;

    @Autowired
    private VideoDetectionRepository videoDetectionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private VideoJobProcessorService videoJobProcessorService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public VideoJobDto createJobAndStartProcessing(
            MultipartFile video,
            Integer frameStep,
            Integer maxFrames,
            Authentication authentication
    ) throws IOException {
        AppUser currentUser = getCurrentUser(authentication);
        StoredVideo storedVideo = storeVideo(video);

        VideoJob job = new VideoJob();
        job.setUser(currentUser);
        job.setSourceFilename(storedVideo.sourceFilename());
        job.setStoredFilename(storedVideo.storedFilename());
        job.setStoragePath(storedVideo.absolutePath());
        job.setStatus(VideoJobStatus.PENDING);
        job.setProgressPercent(0);
        job.setCreatedAt(LocalDateTime.now());

        VideoJob saved = videoJobRepository.save(job);
        videoJobProcessorService.processJobAsync(
                saved.getId(),
                normalizeFrameStep(frameStep),
                normalizeMaxFrames(maxFrames)
        );

        return toDto(saved);
    }

    public List<VideoJobDto> getCurrentUserJobs(Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        return videoJobRepository.findAllByUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    public VideoJobDto getCurrentUserJobById(Long jobId, Authentication authentication) {
        return toDto(getCurrentUserOwnedJob(jobId, authentication));
    }

    public VideoDetectionPageDto getCurrentUserJobResults(
            Long jobId,
            Integer page,
            Integer size,
            Authentication authentication
    ) {
        VideoJob job = getCurrentUserOwnedJob(jobId, authentication);

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        Page<VideoDetection> detectionPage = videoDetectionRepository
                .findByJob_IdOrderByFrameIndexAscIdAsc(
                        job.getId(),
                        PageRequest.of(normalizedPage, normalizedSize)
                );

        List<VideoDetectionDto> items = detectionPage.getContent()
                .stream()
                .map(this::toDetectionDto)
                .toList();

        return new VideoDetectionPageDto(
                items,
                normalizedPage,
                normalizedSize,
                detectionPage.getTotalElements(),
                detectionPage.getTotalPages()
        );
    }

    public VideoStreamPayload getCurrentUserVideoPayload(Long jobId, Authentication authentication) throws IOException {
        VideoJob job = getCurrentUserOwnedJob(jobId, authentication);
        if (job.getStoragePath() == null || job.getStoragePath().isBlank()) {
            throw new RuntimeException("Video indisponibil pentru acest job.");
        }

        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        Path path = Path.of(job.getStoragePath()).toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot)) {
            throw new RuntimeException("Calea video este invalida.");
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new RuntimeException("Fisierul video nu exista.");
        }

        Resource resource = toUrlResource(path);
        String contentType = Files.probeContentType(path);
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new VideoStreamPayload(resource, contentType, Files.size(path));
    }

    private VideoJob getCurrentUserOwnedJob(Long jobId, Authentication authentication) {
        AppUser currentUser = getCurrentUser(authentication);
        return videoJobRepository.findByIdAndUser_Id(jobId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Job video inexistent"));
    }

    private AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Utilizator neautentificat");
        }

        String username = authentication.getName();
        return appUserRepository.findTopByUsernameIgnoreCaseOrderByIdDesc(username)
                .orElseThrow(() -> new RuntimeException("Utilizatorul nu a fost gasit"));
    }

    private VideoJobDto toDto(VideoJob job) {
        return new VideoJobDto(
                job.getId(),
                job.getSourceFilename(),
                job.getStoredFilename(),
                job.getStatus(),
                job.getProgressPercent(),
                job.getErrorMessage(),
                videoDetectionRepository.countByJob_Id(job.getId()),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }

    private VideoDetectionDto toDetectionDto(VideoDetection detection) {
        VideoDetectionDto.BboxDto bboxDto = new VideoDetectionDto.BboxDto(
                detection.getBboxX(),
                detection.getBboxY(),
                detection.getBboxW(),
                detection.getBboxH()
        );

        return new VideoDetectionDto(
                detection.getId(),
                detection.getFrameIndex(),
                detection.getTimestampMs(),
                detection.getTrackId(),
                detection.getPlateText(),
                detection.getConfidence(),
                bboxDto
        );
    }

    private StoredVideo storeVideo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fisierul video incarcat este gol.");
        }

        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "video-upload.bin");
        String suffix = resolveVideoSuffix(originalName);
        validateVideoFile(file, suffix);

        String uniqueName = "alpro-video-" + UUID.randomUUID() + suffix;
        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        Path videoRoot = uploadRoot.resolve("videos").normalize();
        Files.createDirectories(videoRoot);

        Path targetPath = videoRoot.resolve(uniqueName).normalize();
        if (!targetPath.startsWith(videoRoot)) {
            throw new IOException("Calea de upload video este invalida.");
        }

        file.transferTo(targetPath);
        return new StoredVideo(originalName, uniqueName, targetPath.toString());
    }

    private void validateVideoFile(MultipartFile file, String suffix) {
        Set<String> allowedExtensions = Set.of(".mp4", ".mov", ".avi", ".mkv", ".webm");

        String contentType = file.getContentType();
        boolean isVideoContentType = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("video/");
        boolean allowedByExtension = allowedExtensions.contains(suffix.toLowerCase(Locale.ROOT));

        if (!isVideoContentType && !allowedByExtension) {
            throw new RuntimeException("Se accepta doar fisiere video (mp4, mov, avi, mkv, webm).");
        }
    }

    private String resolveVideoSuffix(String originalName) {
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < originalName.length() - 1) {
            String extension = originalName.substring(lastDot).toLowerCase(Locale.ROOT);
            if (extension.matches("^\\.[a-z0-9]{1,8}$")) {
                return extension;
            }
        }
        return ".bin";
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 100;
        }
        return Math.min(size, 500);
    }

    private Integer normalizeFrameStep(Integer frameStep) {
        if (frameStep == null) {
            return null;
        }
        if (frameStep <= 0) {
            return 1;
        }
        return Math.min(frameStep, 120);
    }

    private Integer normalizeMaxFrames(Integer maxFrames) {
        if (maxFrames == null || maxFrames <= 0) {
            return null;
        }
        return Math.min(maxFrames, 5000);
    }

    private Resource toUrlResource(Path path) {
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Nu s-a putut accesa fisierul video.");
        }
    }

    private record StoredVideo(String sourceFilename, String storedFilename, String absolutePath) {
    }

    public record VideoStreamPayload(Resource resource, String contentType, long contentLength) {
    }
}
