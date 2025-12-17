package com.codeit.mopl.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeit.mopl.config.QuerydslConfig;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
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

@DataJpaTest
@Import(QuerydslConfig.class)
public class NotificationRepositoryImplTest {

  @Autowired
  private TestEntityManager em;

  private User user;           // 모든 테스트 공용

  private Notification n1;
  private Notification n2;
  private Notification n3;
  private Notification n4;
  private Notification n5;
  private Notification n6;

  @Autowired
  private NotificationRepository notificationRepository;

  @MockitoBean
  private ContentMapper contentMapper;

  @BeforeEach
  void setUp() throws Exception {
    user = createUser("test@example.com", "encodedPassword", "test");

    em.persist(user);

    n1 = createNotification(user, "testTitle1", "testContent1", Level.INFO, Status.UNREAD);
    em.persist(n1);

    n2 = createNotification(user, "testTitle2", "testContent2", Level.INFO, Status.UNREAD);
    em.persist(n2);

    n3 = createNotification(user, "testTitle3", "testContent3", Level.INFO, Status.READ);
    em.persist(n3);

    n4 = createNotification(user, "testTitle4", "testContent4", Level.INFO, Status.UNREAD);
    em.persist(n4);

    n5 = createNotification(user, "testTitle5", "testContent5", Level.INFO, Status.UNREAD);
    em.persist(n5);

    n6 = createNotification(user, "testTitle6", "testContent6", Level.INFO, Status.UNREAD);
    em.persist(n6);

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("시간을 기준으로 내림차순 정렬했을 때, Id가 동일하고 status = UNREAD 조건에 맞는 알림만 조회된다")
  void searchNotification_sortedByCreatedAtDesc() {
    // given
    UUID userId = user.getId();

    // when
    List<Notification> result = notificationRepository.searchNotifications(
        userId,
        null,
        null,
        3,
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
    );

    // then
    assertThat(result.get(0).getId())
        .isEqualTo(n6.getId());
    assertThat(result.size()).isEqualTo(4);
  }

  @Test
  @DisplayName("시간을 기준으로 오름차순 정렬했을 때, Id가 동일하고 status = UNREAD 조건에 맞는 알림만 조회된다")
  void searchNotification_sortedByCreatedAtAsc() {
    // given
    UUID userId = user.getId();

    // when
    List<Notification> result = notificationRepository.searchNotifications(
        userId,
        null,
        null,
        3,
        SortDirection.ASCENDING,
        SortBy.CREATED_AT
    );

    // then
    assertThat(result.get(0).getId())
        .isEqualTo(n1.getId());

    assertThat(result.size()).isEqualTo(4);
  }

  @Test
  @DisplayName("cursor와 DESC 정렬이 주어지면, 해당 시간 이전의 UNREAD 알림만 조회된다")
  void searchNotification_withCursorAndDesc() {
    // given
    UUID userId = user.getId();

    String cursor = n4.getCreatedAt().toString();

    // when
    List<Notification> result = notificationRepository.searchNotifications(
        userId,
        cursor,
        n4.getId(),
        3,
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
    );

    // then
    assertThat(result)
        .extracting(Notification::getContent)
        .containsExactly(n2.getContent(), n1.getContent());

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  @DisplayName("cursor와 ASC 정렬이 주어지면, 해당 시간 이후의 UNREAD 알림만 조회된다")
  void searchNotification_withCursorAndAsc() {
    // given
    UUID userId = user.getId();

    String cursor = n4.getCreatedAt().toString();

    // when
    List<Notification> result = notificationRepository.searchNotifications(
        userId,
        cursor,
        n4.getId(),
        3,
        SortDirection.ASCENDING,
        SortBy.CREATED_AT
    );

    // then
    assertThat(result)
        .extracting(Notification::getContent)
        .containsExactly(n5.getContent(), n6.getContent());

    assertThat(result.size()).isEqualTo(2);
  }

  private Notification createNotification(User user, String title, String content, Level level, Status status)
      throws InterruptedException {
      Notification n = new Notification();
      n.setTitle(title);
      n.setContent(content);
      n.setUser(user);
      n.setLevel(level);
      n.setStatus(status);
      Thread.sleep(10);     // sleep 각 객체마다 createdAt의 값에 차이점을 주기 위함
      return n;
  }
  private User createUser(String email, String password, String name) {
    return new User(email, password, name);
  }
}
