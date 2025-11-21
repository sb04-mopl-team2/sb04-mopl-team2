package com.codeit.mopl.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.review.mapper.ReviewMapper;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import com.codeit.mopl.domain.user.dto.response.UserSummary;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.review.ReviewDuplicateException;
import com.codeit.mopl.exception.review.ReviewNotFoundException;
import com.codeit.mopl.exception.review.ReviewForbiddenException;
import com.codeit.mopl.exception.user.UserNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
  private UserRepository userRepository;

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private ReviewMapper reviewMapper;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  @DisplayName("리뷰 목록 조회 - 결과가 비어있으면 data는 빈 리스트, hasNext=false, totalCount=0")
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
    assertThat(result.data()).isNotNull();
    assertThat(result.data()).isEmpty();            // ✔ null → empty list 로 변경됨
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isZero();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.sortBy()).isEqualTo(sortBy.toString());
    assertThat(result.sortDirection()).isEqualTo(sortDirection);

    // ✔ 빈 결과일 때 totalCount 조회 안 하는지 검증
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

  @Test
  @DisplayName("리뷰 생성 성공 - 유효한 요청 시 ReviewDto 반환")
  void createReview_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    String text = "좋은 리뷰입니다!";
    double rating = 4.5;

    User user = new User();
    Content content = new Content();
    Review review = new Review(user, content, text, rating, false);

    UserSummary userSummary = new UserSummary(userId, "유저 이름", "유저 프로필 이미지 url");

    ReviewDto dto = new ReviewDto(
        UUID.randomUUID(),
        contentId,
        userSummary,
        text,
        rating
    );

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
    when(reviewRepository.findByUserAndContent(user, content))
    .thenReturn(Optional.empty());
    when(reviewRepository.save(any(Review.class))).thenReturn(review);
    when(reviewMapper.toDto(any(Review.class))).thenReturn(dto);

    // when
    ReviewDto result = reviewService.createReview(userId, contentId, text, rating);

    // then
    assertThat(result).isNotNull();
    assertThat(result.text()).isEqualTo(text);
    assertThat(result.rating()).isEqualTo(rating);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 유저가 없을 때")
  void createReview_fail_userNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    String text = "좋은 리뷰입니다!";
    double rating = 4.5;

    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // when
    Runnable act = () ->
        reviewService.createReview(userId, contentId, text, rating);

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 이미 리뷰가 있을 때")
  void createReview_fail_reviewDuplicated() {
    // given
    UUID userId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    String text = "좋은 리뷰입니다!";
    double rating = 4.5;
    User user = new User();
    Content content = new Content();

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    UUID reviewId = UUID.randomUUID();
    Review duplicatedReview = mock(Review.class);
    when(duplicatedReview.getId()).thenReturn(reviewId);
    when(reviewRepository.findByUserAndContent(user, content)).thenReturn(Optional.of(duplicatedReview));

    // when
    Runnable act = () ->
        reviewService.createReview(userId, contentId, text, rating);

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(ReviewDuplicateException.class);
  }

  @Test
  @DisplayName("리뷰 수정 성공 - 작성자 본인이면 수정된다")
  void updateReview_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    String newText = "수정된 리뷰입니다.";
    double newRating = 4.8;

    User author = mock(User.class);
    when(author.getId()).thenReturn(userId);

    Review review = new Review(author, new Content(), "old", 3.0, false);

    ReviewDto dto = new ReviewDto(
        reviewId,
        UUID.randomUUID(),
        null,
        newText,
        newRating
    );

    when(reviewRepository.findById(reviewId))
        .thenReturn(Optional.of(review));

    when(reviewRepository.save(any(Review.class)))
        .thenReturn(review);

    when(reviewMapper.toDto(any(Review.class)))
        .thenReturn(dto);

    // when
    ReviewDto result = reviewService.updateReview(
        userId, reviewId, newText, newRating
    );

    // then
    assertThat(review.getText()).isEqualTo(newText);
    assertThat(review.getRating()).isEqualTo(newRating);

    assertThat(result.text()).isEqualTo(newText);
    assertThat(result.rating()).isEqualTo(newRating);

    verify(reviewRepository).findById(reviewId);
    verify(reviewRepository).save(review);
    verify(reviewMapper).toDto(review);
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 작성자가 아니면 예외 발생")
  void updateReview_fail_unauthorized() {
    // given
    UUID requestUserId = UUID.randomUUID();
    UUID authorId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();

    String newText = "수정된 리뷰입니다.";
    double newRating = 4.8;

    User author = mock(User.class);
    when(author.getId()).thenReturn(authorId);

    Review review = mock(Review.class);
    when(review.getUser()).thenReturn(author);

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    // when
    Runnable act = () -> reviewService.updateReview(
        requestUserId, reviewId, newText, newRating
    );

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(ReviewForbiddenException.class);

    verify(reviewRepository).findById(reviewId);
    verify(reviewRepository, never()).save(any());
    verify(reviewMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 리뷰가 없으면 예외 발생")
  void updateReview_fail_reviewNotFound() {
    // given
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    String newText = "수정된 리뷰입니다.";
    double newRating = 4.8;

    when(reviewRepository.findById(reviewId))
        .thenReturn(Optional.empty());

    // when
    Runnable act = () -> reviewService.updateReview(
        userId, reviewId, newText, newRating
    );

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(ReviewNotFoundException.class);

    verify(reviewRepository).findById(reviewId);
    verify(reviewRepository, never()).save(any());
    verify(reviewMapper, never()).toDto(any());
  }
  @Test
  @DisplayName("리뷰 삭제 - 작성자가 본인 리뷰를 삭제하면 isDeleted=true로 저장된다")
  void deleteReview_whenUserIsOwner_shouldSoftDeleteReview()  {
    // given
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();

    User author = mock(User.class);
    when(author.getId()).thenReturn(userId);

    Review review = new Review(author, new Content(), "old", 3.0, false);

    when(reviewRepository.findById(reviewId))
        .thenReturn(Optional.of(review));

    // when
    reviewService.deleteReview(userId, reviewId);

    // then
    assertThat(review.getIsDeleted()).isTrue();
    verify(reviewRepository).save(review);
  }

  @Test
  @DisplayName("리뷰 삭제 - 작성자가 아닌 사용자가 삭제 시도하면 예외를 발생시키고 저장하지 않는다")
  void deleteReview_notOwnerUser_shouldFailWithForbiddenException() {
    // given
    UUID requestUserId = UUID.randomUUID(); // 요청한 유저
    UUID authorId = UUID.randomUUID();      // 실제 작성자
    UUID reviewId = UUID.randomUUID();

    User author = mock(User.class);
    when(author.getId()).thenReturn(authorId);

    Review review = mock(Review.class);
    when(review.getUser()).thenReturn(author);

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    // when
    Runnable act = () -> reviewService.deleteReview(requestUserId, reviewId);

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(ReviewForbiddenException.class);

    verify(reviewRepository).findById(reviewId);
    verify(reviewRepository, never()).save(any());
    verify(review, never()).setIsDeleted(true);
  }

  @Test
  @DisplayName("리뷰 삭제 - 리뷰가 없으면 ReviewNotFoundException을 발생시키고 저장하지 않는다")
  void deleteReview_whenReviewNotFound_shouldFailWithNotFoundException() {
    // given
    UUID requestUserId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();

    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    // when
    Runnable act = () -> reviewService.deleteReview(requestUserId, reviewId);

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(ReviewNotFoundException.class);

    verify(reviewRepository).findById(reviewId);
    verify(reviewRepository, never()).save(any());
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
