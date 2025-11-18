package com.codeit.mopl.domain.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewUpdateRequest(

    @NotNull
    String text,

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    double rating
) {}