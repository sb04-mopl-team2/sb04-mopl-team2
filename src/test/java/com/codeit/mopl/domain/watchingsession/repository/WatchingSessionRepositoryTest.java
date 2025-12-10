package com.codeit.mopl.domain.watchingsession.repository;

import static org.assertj.core.api.Assertions.assertThat;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.watchingsession.entity.WatchingSession;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.codeit.mopl.util.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(QueryDslConfig.class)
public class WatchingSessionRepositoryTest {

  @Autowired
  TestEntityManager em;

  @Autowired
  WatchingSessionRepository watchingSessionRepository;

  @MockitoBean
  ContentMapper contentMapper;

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

    Instant instant1 = Instant.now();
    Instant instant2 = Instant.now().minus(Duration.ofHours(1));

    this.w1 = new WatchingSession();
    w1.setUser(user1);
    w1.setContent(content);
    em.persist(w1);
    ReflectionTestUtils.setField(w1, "createdAt", instant1);
    em.flush();

    this.w2 = new WatchingSession();
    w2.setUser(user2);
    w2.setContent(content);
    em.persist(w2);
    ReflectionTestUtils.setField(w2, "createdAt", instant2);
    em.flush();

    // cache 클리어
    em.clear();
  }

  /*
    List<WatchingSession> findWatchingSessions(...)
   */
  @Test
  @DisplayName("contentId로 성공적으로 조회")
  void getWatchingSessionsWithContentIdWithoutCursorOrIdAfterSuccessful() {
    // when
    List<WatchingSession> nameResults = watchingSessionRepository.findWatchingSessions(
        content.getId(),    // contentId
        null,               // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
    );
    //then
    assertThat(nameResults).hasSize(2);
  }

  @Test
  @DisplayName("contentId와 watcherNameLike로 성공적으로 조회")
  void getWatchingSessionsWithoutCursorOrIdAfterSuccessful() {
    // when
    List<WatchingSession> nameResults = watchingSessionRepository.findWatchingSessions(
        content.getId(),    // contentId
        "testName",         // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
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
        UUID.randomUUID(),  // contentId
        "testName",         // watcherNameLike
        null,               // cursor
        null,               // idAfter
        10,                 // limit
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
    );
    // then
    assertThat(nameResults).isEmpty();
  }

  @Test
  @DisplayName("ASCENDING 리스트 반환 확인")
  void getByContentIdAscSuccessful() {
    // when
    List<WatchingSession> results = watchingSessionRepository.findWatchingSessions(
        content.getId(),  // contentId
        null,             // watcherNameLike
        null,             // cursor
        null,             // idAfter
        10,               // limit
        SortDirection.ASCENDING,
        SortBy.CREATED_AT
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
        content.getId(),  // contentId
        null,             // watcherNameLike
        null,             // cursor
        null,             // idAfter
        10,               // limit
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
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
        content.getId(),
        null,
        null,
        null,
        1,
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
    );
    assertThat(page1).hasSize(1);
    WatchingSession firstResult = page1.get(0);
    assertThat(firstResult.getId()).isEqualTo(w2.getId()); // w2

    // 두번째 페이지
    String cursorTimestamp =
        firstResult.getCreatedAt().toString();
           // .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    UUID cursorId = firstResult.getId();
    List<WatchingSession> page2 = watchingSessionRepository.findWatchingSessions(
        content.getId(),
        null,
        cursorTimestamp, // 마지막 요소 timestamp
        cursorId,        // 마지막 요소 id
        1,
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
    );

    assertThat(page2).hasSize(1);
    assertThat(page2.get(0).getId()).isEqualTo(w1.getId()); // w1
  }

  @Test
  @DisplayName("동일 createdAt 데이터 페이징 (ID 기반 정렬) 확인")
  void getCursorPaginationWithSameCreatedAtSuccessful() {

    User user3 = new User("test3@test.com", "pw", "test3Name");
    em.persistAndFlush(user3);

    WatchingSession w3 = new WatchingSession();
    w3.setUser(user3);
    w3.setContent(content);
    em.persist(w3);
    // w2와 동일
    ReflectionTestUtils.setField(w3, "createdAt", w2.getCreatedAt());
    em.flush();
    em.clear();

    List<WatchingSession> allSessions = watchingSessionRepository.findAll(
        Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"))
    );

    WatchingSession expectedFirst = allSessions.get(0);
    WatchingSession expectedSecond = allSessions.get(1);

    // 탑 1 가져오기
    List<WatchingSession> page1 = watchingSessionRepository.findWatchingSessions(
        content.getId(), null,
        null, null, 1,
        SortDirection.DESCENDING, SortBy.CREATED_AT
    );
    assertThat(page1).hasSize(1);
    assertThat(page1.get(0).getId()).isEqualTo(expectedFirst.getId());
    WatchingSession first = page1.get(0);

    // 1번쨰 아이템 커서로 사용
    String cursorTime =
        first.getCreatedAt().toString();
            //.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    List<WatchingSession> page2 = watchingSessionRepository.findWatchingSessions(
        content.getId(), null,
        cursorTime,
        first.getId(),
        1,
        SortDirection.DESCENDING, SortBy.CREATED_AT
    );

    assertThat(page2).hasSize(1);
    assertThat(page2.get(0).getId()).isEqualTo(expectedSecond.getId());
  }


  @Test
  @DisplayName("WatcherCount 조회")
  void getWatcherCount() {
    // when & then -  userId1
    Long result1 = watchingSessionRepository.getWatcherCount(
        content.getId(),
        null
    );
    assertThat(result1).isEqualTo(2L);

    Long result2 = watchingSessionRepository.getWatcherCount(
        content.getId(),
        "testName"
    );
    assertThat(result2).isEqualTo(1L);

    // when & then - 존재하지 않는 이름
    Long result3 = watchingSessionRepository.getWatcherCount(
        content.getId(),
        "nonExisting"
    );
    assertThat(result3).isEqualTo(0L);
  }
}
