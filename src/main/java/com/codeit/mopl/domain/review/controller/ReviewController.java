package com.codeit.mopl.domain.review.controller;

import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewSearchRequestDto;
import com.codeit.mopl.domain.review.service.ReviewService;
import com.codeit.mopl.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  @GetMapping
  public ResponseEntity<CursorResponseReviewDto> findReviews(
      @AuthenticationPrincipal CustomUserDetails user,
      @Validated ReviewSearchRequestDto request
  ) {

    log.info("[리뷰] 알림 조회 요청 시작, userId = {}", user.getUser().id());

    CursorResponseReviewDto response = reviewService.findReviews(
        request.contentId(),
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortDirection(),
        request.sortBy()
    );

    log.info("[리뷰] 알림 조회 요청 종료");
    return ResponseEntity.ok(response);
  }

}
