package com.kumbu.backend.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitReviewRequest {

    @NotNull(message = "Rating é obrigatório")
    @Min(value = 1, message = "Rating mínimo é 1")
    @Max(value = 5, message = "Rating máximo é 5")
    private Integer rating;

    @Size(max = 2000, message = "Comentário demasiado longo")
    private String comment;
}
