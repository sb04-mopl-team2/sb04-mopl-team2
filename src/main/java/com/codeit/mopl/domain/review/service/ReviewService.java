package com.codeit.mopl.domain.review.service;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.review.mapper.ReviewMapper;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.review.ReviewDuplicateException;
import com.codeit.mopl.exception.review.ReviewErrorCode;
import com.codeit.mopl.exception.review.ReviewNotFoundException;
import com.codeit.mopl.exception.review.ReviewForbiddenException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;

  private final StringRedisTemplate stringRedisTemplate;

  public static final String REVIEWS_FIRST_PAGE = "review:first-page";

  @Transactional
  public ReviewDto createReview(UUID userId, UUID contentId, String text, double rating) {
    log.info("[리뷰] 리뷰 생성 시작, userId = {}, contentId = {}, text = {}, rating = {}", userId, contentId, text, rating);
    User user = getValidUserByUserId(userId);
    Content content = getValidContentByContentId(contentId);
    checkReviewDuplicate(user, content);
    Review review = new Review(user, content, text, rating, false);
    reviewRepository.save(review);
    log.info("[리뷰] 리뷰 생성 종료, userId = {}, contentId = {}, reviewId = {}", userId, contentId, review.getId());
    evictFirstPageCacheByContentId(contentId);
    return reviewMapper.toDto(review);
  }

  @Transactional
  public ReviewDto updateReview(UUID userId, UUID reviewId, String text, double rating) {
    log.info("[리뷰] 리뷰 수정 시작, userId = {}, reviewId = {}, text = {}, rating = {}", userId, reviewId, text, rating);

    Review review = getValidReviewByReviewId(reviewId);
    if (!review.getUser().getId().equals(userId)) {
      log.warn("[리뷰] 리뷰를 수정할 권한이 없습니다. reviewId = {}", reviewId);
      throw new ReviewForbiddenException(
          ReviewErrorCode.REVIEW_FORBIDDEN, Map.of("reviewId", reviewId));
    }
    review.setText(text);
    review.setRating(rating);
    reviewRepository.save(review);

    log.info("[리뷰] 리뷰 수정 종료, reviewId = {}", reviewId);
    evictFirstPageCacheByContentId(review.getContent().getId());
    return reviewMapper.toDto(review);
  }

  @Transactional
  public void deleteReview(UUID userId, UUID reviewId) {
    log.info("[리뷰] 리뷰 삭제 시작, userId = {}, reviewId = {}", userId, reviewId);
    Review review = getValidReviewByReviewId(reviewId);
    if (!review.getUser().getId().equals(userId)) {
      log.warn("[리뷰] 리뷰를 삭제할 권한이 없습니다. reviewId = {}", reviewId);
      throw new ReviewForbiddenException(
          ReviewErrorCode.REVIEW_FORBIDDEN, Map.of("reviewId", reviewId));
    }
    review.setIsDeleted(true);
    reviewRepository.save(review);
    evictFirstPageCacheByContentId(review.getContent().getId());
    log.info("[리뷰] 리뷰 삭제 종료, reviewId = {}", reviewId);
  }

  @Cacheable(
      cacheNames = REVIEWS_FIRST_PAGE,
      key = "T(java.lang.String).format('%s:%s:%s:%s', #contentId, #limit, #sortDirection, #sortBy)",
      condition = "#cursor == null && #idAfter == null"
  )
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

    if (reviewList.isEmpty()) {
      CursorResponseReviewDto dto = new CursorResponseReviewDto(
          List.of(),
          null,
          null,
          false,
          0L,
          sortBy.toString(),
          sortDirection.toString()
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
        sortBy.toString(),
        sortDirection.toString()
    );

    log.info("[리뷰] 리뷰 조회 종료, contentId = {}, reviewListSize = {}, hasNext = {}, totalCount = {}",
        contentId, data.size(), dto.hasNext(), dto.totalCount());

    return dto;
  }

  private User getValidUserByUserId(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("[리뷰] 해당 유저를 찾을 수 없음 userId = {}", userId);
          return new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId));
        });
  }
  
  private Content getValidContentByContentId(UUID contentId) {
    return contentRepository.findById(contentId).orElseThrow(() -> {
      log.warn("[리뷰] 해당 컨텐츠를 찾을 수 없음 contentId = {}", contentId);
      return new IllegalArgumentException();// 추후에 ContentNotFoundException으로 변경
    });
  }

  private Review getValidReviewByReviewId(UUID reviewId) {
    return reviewRepository.findById(reviewId)
        .orElseThrow(() -> {
          log.warn("[리뷰] 해당 리뷰를 찾을 수 없음 reviewId = {}", reviewId);
          return new ReviewNotFoundException(
              ReviewErrorCode.REVIEW_NOT_FOUND, Map.of("reviewId", reviewId));
        });
  }

  private void checkReviewDuplicate(User user, Content content) {
    Optional<Review> review = reviewRepository.findByUserAndContent(user, content);
    if (review.isPresent()) {
      log.warn("[리뷰] 이미 리뷰가 존재합니다. reviewId = {}", review.get().getId());
      throw new ReviewDuplicateException(
          ReviewErrorCode.REVIEW_DUPLICATED, Map.of("reviewId", review.get().getId()));
    }
  }

  private void evictFirstPageCacheByContentId(UUID contentId) {
    String pattern = REVIEWS_FIRST_PAGE + "::" + contentId + ":*";

    Set<String> keys = stringRedisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      stringRedisTemplate.delete(keys);
      log.info("[리뷰] 리뷰 조회 캐싱 초기화, contentId = {}, evictedKeys = {}", contentId, keys.size());
    }
  }
}