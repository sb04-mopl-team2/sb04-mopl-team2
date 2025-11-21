package com.codeit.mopl.domain.review.dto;

import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import java.util.List;
import java.util.UUID;

public record CursorResponseReviewDto(
    List<ReviewDto> data,      // 데이터 목록
    String nextCursor,         // 다음 커서
    UUID nextIdAfter,          // 다음 요청의 보조 커서
    boolean hasNext,           // 다음 데이터가 있는지 여부
    long totalCount,           // 총 데이터 개수
    String sortBy,             // 정렬 기준
    String sortDirection // 정렬 방향
) { }
