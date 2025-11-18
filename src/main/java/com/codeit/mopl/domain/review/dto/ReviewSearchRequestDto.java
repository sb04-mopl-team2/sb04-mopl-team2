package com.codeit.mopl.domain.review.dto;

import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record ReviewSearchRequestDto(

    UUID contentId,

    String cursor,

    UUID idAfter,

    @Min(1)
    @Max(100)
    int limit,

    @NotNull
    SortDirection sortDirection,

    @NotNull
    ReviewSortBy sortBy

) {}