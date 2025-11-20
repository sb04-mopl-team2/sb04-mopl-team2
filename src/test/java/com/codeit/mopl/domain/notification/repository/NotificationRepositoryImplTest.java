package com.codeit.mopl.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

import com.codeit.mopl.config.QuerydslConfig;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.NotificationSortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
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

@DataJpaTest
@Import(QuerydslConfig.class) // JPAQueryFactory 빈 등록한 설정
public class NotificationRepositoryImplTest {

  @Autowired
  private TestEntityManager em;

  private User user;           // 모든 테스트 공용

  private Notification n1;
  private Notification n2;
  private Notification n3;
  private Notification n4;

  @Autowired
  private NotificationRepository notificationRepository;

  @BeforeEach
  void setUp() throws Exception {
    user = createUser("test@example.com", "encodedPassword", "test");

    em.persist(user);

    n1 = createNotification(user, "testTitle1", "testContent1", Level.INFO, Status.UNREAD);
    em.persist(n1);
    Thread.sleep(5);

    n2 = createNotification(user, "testTitle2", "testContent2", Level.INFO, Status.UNREAD);
    em.persist(n2);
    Thread.sleep(5);

    n3 = createNotification(user, "testTitle3", "testContent3", Level.INFO, Status.READ);
    em.persist(n3);
    Thread.sleep(5);

    n4 = createNotification(user, "testTitle4", "testContent4", Level.INFO, Status.UNREAD);
    em.persist(n4);

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
        10,
        SortDirection.DESCENDING,
        NotificationSortBy.CREATED_AT
    );

    // then
    assertThat(result.get(0).getId())
        .isEqualTo(n4.getId());
    assertThat(result.size()).isEqualTo(3);
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
        10,
        SortDirection.ASCENDING,
        NotificationSortBy.CREATED_AT
    );

    // then
    assertThat(result.get(0).getId())
        .isEqualTo(n1.getId());

    assertThat(result.size()).isEqualTo(3);
  }

  private Notification createNotification(User user, String title, String content, Level level, Status status) {
      Notification n = new Notification();
      n.setTitle(title);
      n.setContent(content);
      n.setUser(user);
      n.setLevel(level);
      n.setStatus(status);
      return n;
  }
  private User createUser(String email, String password, String name) {
    return new User(email, password, name);
  }
}
