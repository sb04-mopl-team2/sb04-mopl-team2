package com.codeit.mopl.domain.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ContentCreateRequest(
    @NotBlank
    String type,

    @NotBlank
    String title,

    @NotBlank
    String description,

    @NotEmpty
    List<String> tags
) {}