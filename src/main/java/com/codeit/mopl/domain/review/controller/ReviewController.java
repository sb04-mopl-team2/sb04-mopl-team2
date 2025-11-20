package com.codeit.mopl.domain.review.controller;

import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewCreateRequest;
import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewSearchRequest;
import com.codeit.mopl.domain.review.dto.ReviewUpdateRequest;
import com.codeit.mopl.domain.review.service.ReviewService;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
      @Validated ReviewSearchRequest request
  ) {

    log.info("[리뷰] 리뷰 조회 요청 시작, userId = {}", user.getUser().id());

    CursorResponseReviewDto response = reviewService.findReviews(
        request.contentId(),
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortDirection(),
        request.sortBy()
    );

    log.info("[리뷰] 리뷰 조회 요청 종료");
    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<ReviewDto> createReview(
      @AuthenticationPrincipal CustomUserDetails user,
      @Validated @RequestBody ReviewCreateRequest request
  ) {

    log.info("[리뷰] 리뷰 생성 요청 시작, userId = {}", user.getUser().id());

    UUID userId = user.getUser().id();
    ReviewDto reviewDto = reviewService.createReview(userId, request.contentId(), request.text(), request.rating());

    log.info("[리뷰] 리뷰 생성 요청 종료, userId = {}", user.getUser().id());
    return ResponseEntity.ok(reviewDto);
  }


  @PatchMapping("/{reviewId}")
  public ResponseEntity<ReviewDto> updateReview(
      @PathVariable("reviewId") UUID reviewId,
      @AuthenticationPrincipal CustomUserDetails user,
      @Valid @RequestBody ReviewUpdateRequest request
  ) {
    log.info("[리뷰] 리뷰 수정 요청 시작, userId = {}, reviewId = {}", user.getUser().id(), reviewId);

    ReviewDto reviewDto = reviewService.updateReview(user.getUser().id(), reviewId, request.text(), request.rating());

    log.info("[리뷰] 리뷰 수정 요청 종료, userId = {}, reviewId = {}", user.getUser().id(), reviewId);
    return ResponseEntity.ok(reviewDto);
  }


  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable("reviewId") UUID reviewId,
      @AuthenticationPrincipal CustomUserDetails user
  ) {
    log.info("[리뷰] 리뷰 삭제 요청 시작, userId = {}, reviewId = {}", user.getUser().id(), reviewId);
    reviewService.deleteReview(user.getUser().id(), reviewId);
    log.info("[리뷰] 리뷰 삭제 요청 종료, userId = {}, reviewId = {}", user.getUser().id(), reviewId);
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }
}
