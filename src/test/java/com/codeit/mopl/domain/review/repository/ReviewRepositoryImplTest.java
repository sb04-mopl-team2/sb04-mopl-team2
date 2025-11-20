package com.codeit.mopl.domain.review.repository;

import com.codeit.mopl.config.QuerydslConfig;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DataJpaTest
@Import(QuerydslConfig.class) // JPAQueryFactory 빈 등록한 설정
class ReviewRepositoryImplTest {

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

  @BeforeEach
  void setUp() throws Exception {
    // 이미 만들어둔 헬퍼 메소드 사용 (같은 테스트 클래스 안에 있다고 가정)
    content1 = createContent("테스트 제목1", "테스트 설명1", ContentType.TV);
    content2 = createContent("테스트 제목2", "테스트 설명2", ContentType.MOVIE);
    user = createUser("test@example.com", "encodedPassword", "test");

    em.persist(user);
    em.persist(content1);
    em.persist(content2);

    // 리뷰 4개 생성 (헬퍼 메소드 사용)
    r1 = createReview(user, content1, 4.0, false); // 대상 content, NOT deleted
    em.persist(r1);
    Thread.sleep(5);

    r2 = createReview(user, content1, 5.0, true);  // 대상 content, BUT deleted
    em.persist(r2);
    Thread.sleep(5);

    r3 = createReview(user, content2, 3.0, false); // 다른 content
    em.persist(r3);
    Thread.sleep(5);

    r4 = createReview(user, content1, 3.5, false); // 대상 content, 가장 나중에 생성 (가장 최신)
    em.persist(r4);

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("시간을 기준으로 내림차순 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByCreatedAtDesc() throws Exception {
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
    assertThat(result.get(0).getId())
        .isEqualTo(r4.getId()); // 최신 createdAt이 가장 먼저 와야 함
  }

  @Test
  @DisplayName("시간을 기준으로 오름차순으로 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByCreatedAtAsc() throws Exception {
    // given
    UUID contentId1 = content1.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        null,
        null,
        10,
        SortDirection.ASCENDING,
        ReviewSortBy.createdAt
    );

    // then
    assertThat(result.get(0).getId())
        .isEqualTo(r1.getId()); // 최신 createdAt이 가장 먼저 와야 함
  }

  @Test
  @DisplayName("평점을 기준으로 내림차순으로 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByRatingDesc() throws Exception {
    // given
    UUID contentId1 = content1.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        null,
        null,
        10,
        SortDirection.DESCENDING,
        ReviewSortBy.rating
    );

    // then
    assertThat(result.get(0).getId())
        .isEqualTo(r1.getId()); // 최신 createdAt이 가장 먼저 와야 함
  }

  @Test
  @DisplayName("평점을 기준으로 오름차순으로 정렬했을 때, contentId와 isDeleted=false 조건에 맞는 리뷰만 조회된다")
  void searchReview_sortedByRatingAsc() throws Exception {
    // given
    UUID contentId1 = content1.getId();

    // when
    List<Review> result = reviewRepository.searchReview(
        contentId1,
        null,
        null,
        10,
        SortDirection.ASCENDING,
        ReviewSortBy.rating
    );

    // then
    assertThat(result.get(0).getId())
        .isEqualTo(r4.getId()); // 최신 createdAt이 가장 먼저 와야 함
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
