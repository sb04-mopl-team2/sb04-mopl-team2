package com.codeit.mopl.domain.review.dto;

import com.codeit.mopl.domain.user.dto.response.UserSummary;
import java.util.UUID;

public record ReviewDto(
    UUID id,            // 리뷰 ID
    UUID contentId,     // 콘텐츠 ID
    UserSummary author, // 작성자 정보
    String text,        // 리뷰 내용
    double rating       // 평점 (0.0 ~ 5.0)
) {
}