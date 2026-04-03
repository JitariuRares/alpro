package com.placute.ocrbackend.dto;

import java.util.List;

public class VideoDetectionPageDto {

    private List<VideoDetectionDto> items;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;

    public VideoDetectionPageDto(
            List<VideoDetectionDto> items,
            Integer page,
            Integer size,
            Long totalElements,
            Integer totalPages
    ) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<VideoDetectionDto> getItems() {
        return items;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }
}
