package com.kumbu.backend.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminIdentityReviewRequest {

    @Size(max = 500)
    private String note;
}
