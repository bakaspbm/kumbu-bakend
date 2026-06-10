package com.kumbu.backend.dto.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageResponse<T> {
    private java.util.List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
