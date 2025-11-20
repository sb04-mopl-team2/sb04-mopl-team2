package com.codeit.mopl.domain.watchingsession.repository;

import static org.assertj.core.api.Assertions.assertThat;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.codeit.mopl.util.QueryDslConfig;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

@DataJpaTest
@Import(QueryDslConfig.class)
public class WatchingSessionRepositoryTest {

  @Autowired
  TestEntityManager em;

  @Autowired
  WatchingSessionRepository watchingSessionRepository;

  private User user1;
  private User user2;
  private Content content;
  private WatchingSession w1;
  private WatchingSession w2;

  @BeforeEach
  void init() {
    this.user1 = new User("test1@test.com", "pw", "testName");
    em.persistAndFlush(user1);
    this.user2 = new User("test2@test.com", "pw", "test2Name");
    em.persistAndFlush(user2);

    this.content = new Content();
    this.content.setTitle("Test Movie");
    this.content.setDescription("Fun movie");
    this.content.setContentType(ContentType.MOVIE);
    em.persistAndFlush(content);

    LocalDateTime localDateTime1 = LocalDateTime.now();
    LocalDateTime localDateTime2 = localDateTime1.plusSeconds(2);

    this.w1 = new WatchingSession();
    w1.setUser(user1);
    w1.setContent(content);
    ReflectionTestUtils.setField(w1, "createdAt", localDateTime1);
    em.persistAndFlush(w1);

    this.w2 = new WatchingSession();
    w2.setUser(user2);
    w2.setContent(content);
    ReflectionTestUtils.setField(w2, "createdAt", localDateTime2);
    em.persistAndFlush(w2);
  }

  /*
    List<WatchingSession> findWatchingSessions(...)
   */
  @Test
  @DisplayName("contentId로 성공적으로 조회")
  void getWatchingSessionsWithoutCursorOrIdAfterSuccessful() {
    // when
    List<WatchingSession> nameResults = watchingSessionRepository.findWatchingSessions(
        null,         // userId
        content.getId(),    // contentId
        "testName",         // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.DESCENDING,
        SortBy.createdAt
    );
    //then
    assertThat(nameResults).hasSize(1);
    assertThat(nameResults).extracting("user.name").containsOnly("testName");
  }

  @Test
  @DisplayName("존재하지 않는 contentId는 빈 리스트 반환")
  void getEmptyListWithNonExistentContentIdSuccessful() {
    // when
    List<WatchingSession> nameResults = watchingSessionRepository.findWatchingSessions(
        null,         // userId
        UUID.randomUUID(),  // contentId
        "testName",         // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.DESCENDING,
        SortBy.createdAt
    );
    // then
    assertThat(nameResults).isEmpty();
  }

  @Test
  @DisplayName("userId로 리스트 조회와 반환")
  void getListUserIdSuccessful() {
    // when
    List<WatchingSession> results = watchingSessionRepository.findWatchingSessions(
        user1.getId(),         // userId
        content.getId(),  // contentId
        null,         // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.DESCENDING,
        SortBy.createdAt
    );
    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getId()).isEqualTo(w1.getId());
    assertThat(results.get(0).getUser().getId()).isEqualTo(user1.getId());
  }

  @Test
  @DisplayName("ASCENDING 리스트 반환 확인")
  void getByContentIdAscSuccessful() {
    // when
    List<WatchingSession> results = watchingSessionRepository.findWatchingSessions(
        null,         // userId
        content.getId(),  // contentId
        null,         // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.ASCENDING,
        SortBy.createdAt
    );
    // then
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getId()).isEqualTo(w1.getId());
    assertThat(results.get(1).getId()).isEqualTo(w2.getId());
  }

  @Test
  @DisplayName("DESCENDING 리스트 반환 확인")
  void getByContentIdDescSuccessful() {
    // when
    List<WatchingSession> results = watchingSessionRepository.findWatchingSessions(
        null,         // userId
        content.getId(),  // contentId
        null,         // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.DESCENDING,
        SortBy.createdAt
    );
    // then
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getId()).isEqualTo(w2.getId());
    assertThat(results.get(1).getId()).isEqualTo(w1.getId());
  }

  @Test
  @DisplayName("Cursor 기반 페이지네이션 확인 (DESC 정렬)")
  void getWatchingSessionsWithCursorPaginationSuccessful() {
    // 1st page
    List<WatchingSession> page1 = watchingSessionRepository.findWatchingSessions(
        null,
        content.getId(),
        null,
        null,
        null,
        1,
        SortDirection.DESCENDING,
        SortBy.createdAt
    );
    assertThat(page1).hasSize(1);
    WatchingSession firstResult = page1.get(0);
    assertThat(firstResult.getId()).isEqualTo(w2.getId()); // get w2

    // 2nd page (cursor test)
    String cursorTimestamp = firstResult.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    UUID cursorId = firstResult.getId();
    List<WatchingSession> page2 = watchingSessionRepository.findWatchingSessions(
        null,
        content.getId(),
        null,
        cursorTimestamp, // last elem timestamp
        cursorId,        // last elem id
        1,
        SortDirection.DESCENDING,
        SortBy.createdAt
    );

    assertThat(page2).hasSize(1);
    assertThat(page2.get(0).getId()).isEqualTo(w1.getId()); // get w1
  }

  @Test
  @DisplayName("동일 createdAt 데이터 페이징 (ID 기반 정렬) 확인")
  void getCursorPaginationWithSameCreatedAtSuccessful() {

    User user3 = new User("test3@test.com", "pw", "test3Name");
    em.persistAndFlush(user3);
    WatchingSession w3 = new WatchingSession();
    w3.setUser(user3);
    w3.setContent(content);
    // same time as w2
    ReflectionTestUtils.setField(w3, "createdAt", w2.getCreatedAt());
    em.persist(w3);
    em.flush();
    em.clear();

    WatchingSession expectedFirst;
    WatchingSession expectedSecond;

    if (w3.getId().compareTo(w2.getId()) > 0) {
      // w3 larger -> comes 1st in DESC
      expectedFirst = w3;
      expectedSecond = w2;
    } else {
      // w2 larger
      expectedFirst = w2;
      expectedSecond = w3;
    }

    // get top 1
    List<WatchingSession> page1 = watchingSessionRepository.findWatchingSessions(
        null, content.getId(), null,
        null, null, 1,
        SortDirection.DESCENDING, SortBy.createdAt
    );
    assertThat(page1).hasSize(1);
    assertThat(page1.get(0).getId()).isEqualTo(expectedFirst.getId());
    WatchingSession first = page1.get(0);

    // use 1st item as cursor
    String cursorTime = first.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    List<WatchingSession> page2 = watchingSessionRepository.findWatchingSessions(
        null, content.getId(), null,
        cursorTime, // <
        first.getId(), // <
        1,
        SortDirection.DESCENDING, SortBy.createdAt
    );

    assertThat(page2).hasSize(1);
    assertThat(page2.get(0).getId()).isEqualTo(w2.getId());
    assertThat(page2.get(0).getId()).isEqualTo(expectedSecond.getId());
  }

  /*
    long getWatcherCount()
   */
  @Test
  @DisplayName("WatcherCount 조회")
  void getWatcherCount() {
    // when & then - total count
    Long result1 = watchingSessionRepository.getWatcherCount(
        content.getId(),
        null,
        null
    );
    assertThat(result1).isEqualTo(2L);

    // when & then - with userId1
    Long result2 = watchingSessionRepository.getWatcherCount(
        content.getId(),
        user1.getId(),
        null
    );
    assertThat(result2).isEqualTo(1L);

    // when & then - non existing name
    Long result3 = watchingSessionRepository.getWatcherCount(
        content.getId(),
        null,
        "nonExistingName"
    );
    assertThat(result3).isEqualTo(0L);
  }
}
