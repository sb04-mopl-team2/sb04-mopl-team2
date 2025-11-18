package com.codeit.mopl.domain.review.service;

import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.review.mapper.ReviewMapper;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {
  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;

  public CursorResponseReviewDto findReviews(
      UUID contentId,
      String cursor,
      UUID idAfter,
      int limit,
      SortDirection sortDirection,
      ReviewSortBy sortBy
  ) {
    List<Review> reviewList =
        reviewRepository.searchReview(contentId, cursor, idAfter, limit, sortDirection, sortBy);

    String sortByValue = (sortBy != null) ? sortBy.toString() : null;

    if (reviewList.isEmpty()) {
      CursorResponseReviewDto dto = new CursorResponseReviewDto(
          List.of(),
          null,
          null,
          false,
          0L,
          sortByValue,
          sortDirection
      );

      log.info("[리뷰] 리뷰 조회 종료, contentId = {}, reviewListSize = {}, hasNext = {}, totalCount = {}",
          contentId, 0, dto.hasNext(), dto.totalCount());

      return dto;
    }

    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = false;

    if (reviewList.size() > limit) {
      reviewList = reviewList.subList(0, limit);
      Review last = reviewList.get(limit - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
      hasNext = true;
    }

    List<ReviewDto> data = reviewList.stream()
        .map(reviewMapper::toDto)
        .toList();

    long totalCount = reviewRepository.countByContentIdAndIsDeleted(contentId, false);

    CursorResponseReviewDto dto = new CursorResponseReviewDto(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        sortByValue,
        sortDirection
    );

    log.info("[리뷰] 리뷰 조회 종료, contentId = {}, reviewListSize = {}, hasNext = {}, totalCount = {}",
        contentId, data.size(), dto.hasNext(), dto.totalCount());

    return dto;
  }

}
