package com.placute.ocrbackend.controller;

import com.placute.ocrbackend.dto.VideoDetectionPageDto;
import com.placute.ocrbackend.dto.VideoJobDto;
import com.placute.ocrbackend.service.VideoJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/video-jobs")
public class VideoJobController {

    @Autowired
    private VideoJobService videoJobService;

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @PostMapping
    public ResponseEntity<VideoJobDto> uploadVideo(
            @RequestParam("video") MultipartFile video,
            @RequestParam(value = "frameStep", required = false) Integer frameStep,
            @RequestParam(value = "maxFrames", required = false) Integer maxFrames,
            Authentication authentication
    ) throws IOException {
        VideoJobDto created = videoJobService.createJobAndStartProcessing(video, frameStep, maxFrames, authentication);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(created);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @GetMapping
    public ResponseEntity<List<VideoJobDto>> getCurrentUserJobs(Authentication authentication) {
        return ResponseEntity.ok(videoJobService.getCurrentUserJobs(authentication));
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @GetMapping("/{jobId}")
    public ResponseEntity<VideoJobDto> getCurrentUserJob(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(videoJobService.getCurrentUserJobById(jobId, authentication));
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @GetMapping("/{jobId}/results")
    public ResponseEntity<VideoDetectionPageDto> getCurrentUserJobResults(
            @PathVariable Long jobId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            Authentication authentication
    ) {
        VideoDetectionPageDto results = videoJobService.getCurrentUserJobResults(jobId, page, size, authentication);
        return ResponseEntity.ok(results);
    }

    @PreAuthorize("hasAnyRole('POLICE', 'PARKING')")
    @GetMapping("/{jobId}/video")
    public ResponseEntity<Resource> getCurrentUserVideo(
            @PathVariable Long jobId,
            Authentication authentication
    ) throws IOException {
        VideoJobService.VideoStreamPayload payload = videoJobService.getCurrentUserVideoPayload(jobId, authentication);

        return ResponseEntity.ok()
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .contentLength(payload.contentLength())
                .body(payload.resource());
    }
}
