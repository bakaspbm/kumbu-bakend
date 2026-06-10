package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminLegalDocumentUpsertRequest {

    @Size(max = 200, message = "Título demasiado longo")
    private String title;

    @Size(max = 4000, message = "Introdução demasiado longa")
    private String intro;

    private List<Map<String, Object>> sections;
}
