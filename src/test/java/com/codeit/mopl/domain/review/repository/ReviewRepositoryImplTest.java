package com.codeit.mopl.domain.review.repository;

import com.codeit.mopl.config.QuerydslConfig;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.user.entity.User;
import java.lang.reflect.Field;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class) // JPAQueryFactory 빈 등록한 설정
class ReviewRepositoryImplTest {

  @MockitoBean
  private ContentMapper contentMapper;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private TestEntityManager em;

  private User user;
  private Content content1;
  private Content content2;

  private Review r1;
  private Review r2;
  private Review r3;
  private Review r4;
  private Review r5;
  private Review r6;

  @BeforeEach
  void setUp() throws Exception {
    content1 = createContent("테스트 제목1", "테스트 설명1");
    content2 = createContent("테스트 제목2", "테스트 설명2");
    user = createUser("test@example.com", "encodedPassword", "test");

    em.persist(user);
    em.persist(content1);
    em.persist(content2);

    r1 = createReview(user, "text1", content1, 4.0, false);
    em.persist(r1);

    r2 = createReview(user, "text2", content1, 5.0, true);
    em.persist(r2);

    r3 = createReview(user, "text3", content2, 3.0, false);
    em.persist(r3);

    r4 = createReview(user, "text4", content1, 3.5, false);
    em.persist(r4);

    r5 = createReview(user, "text5", content1, 3.8, false);
    em.persist(r5);

    r6 = createReview(user, "text6", content1, 3.7, false);
    em.persist(r6);

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
        3,
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
  void searchReview_withRatingCursorAndDesc() {
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
  void searchReview_withRatingCursorAndAsc() {
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
  void searchReview_withCreatedAtCursorAndDesc() {
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
  void searchReview_withCreatedAtCursorAndAsc() {
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

  private Content createContent(String title, String description) {
    Content content = new Content();
    content.setTitle(title);
    content.setDescription(description);
    content.setContentType(ContentType.TV);
    return content;
  }

  private Review createReview(User user,String text, Content content, double rating, boolean isDeleted)
      throws InterruptedException {
    Review review = new Review();
    review.setUser(user);
    review.setContent(content);
    review.setText(text);
    review.setRating(rating);
    review.setIsDeleted(isDeleted);
    Thread.sleep(100);     // sleep 각 객체마다 createdAt의 값에 차이점을 주기 위함
    return review;
  }

  private void setCreatedAt(Review review, LocalDateTime createdAt) {
    Field field = ReflectionUtils.findField(Review.class, "createdAt");
    if (field == null) {
      throw new IllegalStateException("createdAt 필드를 찾을 수 없습니다.");
    }
    field.setAccessible(true);
    ReflectionUtils.setField(field, review, createdAt);
  }
}
