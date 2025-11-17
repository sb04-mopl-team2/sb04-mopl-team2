package com.codeit.mopl.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.review.mapper.ReviewMapper;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import com.codeit.mopl.domain.review.service.ReviewService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewMapper reviewMapper;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  @DisplayName("리뷰 목록 조회 - 결과가 비어있으면 data=null, hasNext=false, totalCount=0")
  void findReviews_whenEmptyResult_shouldReturnEmptyCursorResponse() {
    // given
    UUID contentId = UUID.randomUUID();
    String cursor = null;
    UUID idAfter = null;
    int limit = 10;
    SortDirection sortDirection = SortDirection.DESCENDING;
    ReviewSortBy sortBy = ReviewSortBy.createdAt;

    when(reviewRepository.searchReview(
        eq(contentId),
        eq(cursor),
        eq(idAfter),
        eq(limit),
        eq(sortDirection),
        eq(sortBy)
    )).thenReturn(List.of());

    // when
    CursorResponseReviewDto result = reviewService.findReviews(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    );

    // then
    assertThat(result.data()).isNull();
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isZero();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.sortBy()).isEqualTo(sortBy.toString());
    assertThat(result.sortDirection()).isEqualTo(sortDirection);

    // 빈 결과일 땐 totalCount 조회 안 하는지 확인 (현재 구현 그대로라면)
    verify(reviewRepository, never()).countByContentIdAndIsDeleted(any(), any());
  }

  @Test
  @DisplayName("리뷰 목록 조회 - 결과 개수가 limit 이하이면 hasNext=false, nextCursor/nextIdAfter=null")
  void findReviews_whenResultSizeLessOrEqualLimit_shouldNotHaveNext() {
    // given
    UUID contentId = UUID.randomUUID();
    String cursor = null;
    UUID idAfter = null;
    int limit = 2;
    SortDirection sortDirection = SortDirection.DESCENDING;
    ReviewSortBy sortBy = ReviewSortBy.createdAt;

    Review review1 = createReview(UUID.randomUUID(), LocalDateTime.now().minusMinutes(2));
    Review review2 = createReview(UUID.randomUUID(), LocalDateTime.now().minusMinutes(1));
    List<Review> reviews = List.of(review1, review2);

    when(reviewRepository.searchReview(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    )).thenReturn(reviews);

    ReviewDto dto1 = new ReviewDto(
        review1.getId(), contentId, null, "text1", 4.5
    );
    ReviewDto dto2 = new ReviewDto(
        review2.getId(), contentId, null, "text2", 3.0
    );

    when(reviewMapper.toDto(review1)).thenReturn(dto1);
    when(reviewMapper.toDto(review2)).thenReturn(dto2);
    when(reviewRepository.countByContentIdAndIsDeleted(contentId, false))
        .thenReturn(2L);

    // when
    CursorResponseReviewDto result = reviewService.findReviews(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    );

    // then
    assertThat(result.data()).hasSize(2);
    assertThat(result.data()).containsExactly(dto1, dto2);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.totalCount()).isEqualTo(2L);
    assertThat(result.sortBy()).isEqualTo(sortBy.toString());
    assertThat(result.sortDirection()).isEqualTo(sortDirection);
  }

  @Test
  @DisplayName("리뷰 목록 조회 - 결과 개수가 limit보다 크면 hasNext=true, nextCursor/nextIdAfter 설정")
  void findReviews_whenResultSizeGreaterThanLimit_shouldHaveNextAndSetCursor() {
    // given
    UUID contentId = UUID.randomUUID();
    String cursor = null;
    UUID idAfter = null;
    int limit = 2;
    SortDirection sortDirection = SortDirection.DESCENDING;
    ReviewSortBy sortBy = ReviewSortBy.createdAt;

    Review review1 = createReview(UUID.randomUUID(), LocalDateTime.now().minusMinutes(3));
    Review review2 = createReview(UUID.randomUUID(), LocalDateTime.now().minusMinutes(2));
    Review review3 = createReview(UUID.randomUUID(), LocalDateTime.now().minusMinutes(1)); // limit+1번째

    List<Review> reviews = new ArrayList<>();
    reviews.add(review1);
    reviews.add(review2);
    reviews.add(review3);

    when(reviewRepository.searchReview(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    )).thenReturn(reviews);

    ReviewDto dto1 = new ReviewDto(
        review1.getId(), contentId, null, "text1", 4.5
    );
    ReviewDto dto2 = new ReviewDto(
        review2.getId(), contentId, null, "text2", 3.0
    );

    when(reviewMapper.toDto(review1)).thenReturn(dto1);
    when(reviewMapper.toDto(review2)).thenReturn(dto2);
    when(reviewRepository.countByContentIdAndIsDeleted(contentId, false))
        .thenReturn(3L);

    // when
    CursorResponseReviewDto result = reviewService.findReviews(
        contentId, cursor, idAfter, limit, sortDirection, sortBy
    );

    // then
    assertThat(result.data()).hasSize(limit);
    assertThat(result.data()).containsExactly(dto1, dto2);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(review2.getCreatedAt().toString());
    assertThat(result.nextIdAfter()).isEqualTo(review2.getId());
    assertThat(result.totalCount()).isEqualTo(3L);
    assertThat(result.sortBy()).isEqualTo(sortBy.toString());
    assertThat(result.sortDirection()).isEqualTo(sortDirection);
  }

  private Review createReview(UUID id, LocalDateTime createdAt) {
    Review review = new Review();
    review.setText("sample");
    review.setRating(4.0);

    // DeletableEntity의 id, createdAt 같은 필드는 Reflection으로 세팅
    ReflectionTestUtils.setField(review, "id", id);
    ReflectionTestUtils.setField(review, "createdAt", createdAt);

    return review;
  }
}
