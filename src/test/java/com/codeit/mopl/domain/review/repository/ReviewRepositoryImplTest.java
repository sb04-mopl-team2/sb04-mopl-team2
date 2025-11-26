package com.codeit.mopl.domain.review.repository;

import com.codeit.mopl.config.QuerydslConfig;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class) // JPAQueryFactory 빈 등록한 설정
class ReviewRepositoryImplTest {

  @MockitoBean
  private ContentMapper contentMapper;

  @Autowired
  private ReviewRepository reviewRepository; // JpaRepository + CustomReviewRepository

  @Autowired
  private TestEntityManager em;

  private User user;           // 모든 테스트 공용
  private Content content1;    // 모든 테스트 공용
  private Content content2;    // 모든 테스트 공용

  private Review r1;
  private Review r2;
  private Review r3;
  private Review r4;
  private Review r5;
  private Review r6;

  @BeforeEach
  void setUp() throws Exception {
    content1 = createContent("테스트 제목1", "테스트 설명1", ContentType.TV);
    content2 = createContent("테스트 제목2", "테스트 설명2", ContentType.MOVIE);
    user = createUser("test@example.com", "encodedPassword", "test");

    em.persist(user);
    em.persist(content1);
    em.persist(content2);

    r1 = createReview(user, content1, 4.0, false);
    em.persist(r1);
    Thread.sleep(1000);

    r2 = createReview(user, content1, 5.0, true);
    em.persist(r2);
    Thread.sleep(1000);

    r3 = createReview(user, content2, 3.0, false);
    em.persist(r3);
    Thread.sleep(1000);

    r4 = createReview(user, content1, 3.5, false);
    em.persist(r4);
    Thread.sleep(1000);

    r5 = createReview(user, content1, 3.8, false);
    em.persist(r5);
    Thread.sleep(1000);

    r6 = createReview(user, content1, 3.7, false);
    em.persist(r6);
    Thread.sleep(1000);

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("시간을 기준으로 내림차순 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByCreatedAtDesc() {
    // given
    UUID contentId1 = content1.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        null,
        null,
        10,
        SortDirection.DESCENDING,
        ReviewSortBy.createdAt
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r6.getRating());

    assertThat(result.size()).isEqualTo(4);
  }

  @Test
  @DisplayName("시간을 기준으로 오름차순으로 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByCreatedAtAsc() {
    // given
    UUID contentId1 = content1.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        null,
        null,
        3,
        SortDirection.ASCENDING,
        ReviewSortBy.createdAt
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r1.getRating());
    assertThat(result.size()).isEqualTo(4);

  }

  @Test
  @DisplayName("평점을 기준으로 내림차순으로 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByRatingDesc() {
    // given
    UUID contentId1 = content1.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        null,
        null,
        3,
        SortDirection.DESCENDING,
        ReviewSortBy.rating
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r1.getRating());

    assertThat(result.size()).isEqualTo(4);
  }

  @Test
  @DisplayName("평점을 기준으로 오름차순으로 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByRatingAsc() {
    // given
    UUID contentId1 = content1.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        null,
        null,
        3,
        SortDirection.ASCENDING,
        ReviewSortBy.rating
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r4.getRating());

    assertThat(result.size()).isEqualTo(4);
  }

  @Test
  @DisplayName("정렬기준이 rating 이고 내림차순 정렬일 때 cursor와 idAfter가 있을 때 커서페이지네이션을 진행한다.")
  void searchNotification_withRatingCursorAndDesc() {
    // given
    UUID contentId1 = content1.getId();
    String cursor = r6.getRating().toString();
    UUID id = r6.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        cursor,
        id,
        3,
        SortDirection.DESCENDING,
        ReviewSortBy.rating
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r4.getRating());

    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("정렬기준이 rating 이고 오름차순 정렬일 때 cursor와 idAfter가 있을 때 커서페이지네이션을 진행한다.")
  void searchNotification_withRatingCursorAndAsc() {
    // given
    UUID contentId1 = content1.getId();
    String cursor = r5.getRating().toString();
    UUID id = r5.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        cursor,
        id,
        3,
        SortDirection.ASCENDING,
        ReviewSortBy.rating
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r1.getRating());

    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("정렬기준이 createdAt 이고 내림차순 정렬일 때 cursor와 idAfter가 있을 때 커서페이지네이션을 진행한다.")
  void searchNotification_withCreatedAtCursorAndDesc() {
    // given
    UUID contentId1 = content1.getId();
    String cursor = r4.getCreatedAt().toString();
    UUID id = r4.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        cursor,
        id,
        3,
        SortDirection.DESCENDING,
        ReviewSortBy.createdAt
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r1.getRating());

    assertThat(result.size()).isEqualTo(1);
  }

  @Test
  @DisplayName("정렬기준이 createdAt 이고 오름차순 정렬일 때 cursor와 idAfter가 있을 때 커서페이지네이션을 진행한다.")
  void searchNotification_withCreatedAtCursorAndAsc() {
    // given
    UUID contentId1 = content1.getId();
    String cursor = r5.getCreatedAt().toString();
    UUID id = r5.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        cursor,
        id,
        3,
        SortDirection.ASCENDING,
        ReviewSortBy.createdAt
    );

    // then
    assertThat(result.get(0).getRating())
        .isEqualTo(r6.getRating());

    assertThat(result.size()).isEqualTo(1);
  }

  // 아래는 헬퍼 메소드
  private User createUser(String email, String password, String name) {
    return new User(email, password, name);
  }

  private Content createContent(String title, String description, ContentType contentType) {
    Content content = new Content();
    content.setTitle(title);
    content.setDescription(description);
    content.setContentType(ContentType.TV);
    return content;
  }

  private Review createReview(User user, Content content, double rating, boolean isDeleted) {
    Review review = new Review();
    review.setUser(user);
    review.setContent(content);
    review.setText("sample");
    review.setRating(rating);
    review.setIsDeleted(isDeleted);
    return review;
  }
}
