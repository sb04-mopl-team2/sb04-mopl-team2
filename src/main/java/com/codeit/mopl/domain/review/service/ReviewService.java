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
  ){
    List<ReviewDto> data = null;
    String nextCursor = null;
    UUID nextIdAfter = null;
    boolean hasNext = false;
    long totalCount = 0;

    List<Review> reviewList = reviewRepository.searchReview(contentId, cursor, idAfter, limit, sortDirection, sortBy);

    if (reviewList.isEmpty()) {
      log.debug("[리뷰] 리뷰 리스트가 비었음, contentId = {}", contentId);
      CursorResponseReviewDto cursorResponseReviewDto =  new CursorResponseReviewDto(data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy.toString(), sortDirection);

      log.info("[리뷰] 알림 조회 종료, contentId = {}, reviewListSize = {}, hasNext = {}, totalCount = {}",
          contentId, 0L, cursorResponseReviewDto.hasNext(), cursorResponseReviewDto.totalCount());
      return cursorResponseReviewDto;
    }


    if (reviewList.size() > limit) {
      log.debug("[리뷰] 리뷰 리스트의 사이즈가 limit 값 보다 큼, limit = {}, ListSize = {}", limit, reviewList.size());
      reviewList = reviewList.subList(0, limit);
      nextCursor = reviewList.get(limit - 1).getCreatedAt().toString();
      nextIdAfter = reviewList.get(limit - 1).getId();
      hasNext = true;
    }

    data = reviewList.stream()
        .map(reviewMapper::toDto)
        .toList();

    totalCount = reviewRepository.countByContentIdAndIsDeleted(contentId, false);

    CursorResponseReviewDto cursorResponseReviewDto = new CursorResponseReviewDto
        (data, nextCursor, nextIdAfter, hasNext, totalCount, sortBy.toString(), sortDirection);


    log.info("[리뷰] 알림 조회 종료, contentId = {}, reviewListSize = {}, hasNext = {}, totalCount = {}",
        contentId, 0L, cursorResponseReviewDto.hasNext(), cursorResponseReviewDto.totalCount());

    return cursorResponseReviewDto;
  }


}
