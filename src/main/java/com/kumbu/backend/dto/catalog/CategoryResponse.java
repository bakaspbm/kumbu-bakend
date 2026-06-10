package com.kumbu.backend.dto.catalog;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {
    private String id;
    private String name;
    private String slug;
    private String iconKey;
    private String accentHex;
    private String kind;
}
