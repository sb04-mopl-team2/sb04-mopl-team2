package com.codeit.mopl.domain.message.directmessage.dto;

import com.codeit.mopl.domain.message.conversation.entity.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DirectMessageSearchCond {

    private String cursor;
    private UUID idAfter;

    @Positive
    @Max(100)
    @NotNull(message = "limit 값은 필수입니다.")
    private Integer limit;

    @NotNull(message = "정렬 방향(sortDirection)은 필수입니다.")
    private SortDirection sortDirection;

    @NotNull(message = "정렬 기준(sortBy)은 필수입니다.")
    private SortBy sortBy;
}
