package com.codeit.mopl.domain.notification;

import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private CacheManager cacheManager;

  private User user1;
  private User user2;

  private Notification n1;
  private Notification n2;
  private Notification n3;
  private Notification n4;
  private Notification n5;

  private UserDto userDto1;
  private UserDto userDto2;

  private CustomUserDetails customUserDetails1;
  private CustomUserDetails customUserDetails2;

  @BeforeEach
  void setUp() throws Exception {
    // 혹시 모를 이전 데이터 정리 (선택)
    notificationRepository.deleteAll();
    userRepository.deleteAll();

    user1 = createUser("test@example.com", "encodedPassword", "test");
    user1 = userRepository.saveAndFlush(user1);

    user2 = createUser("test2@example.com", "encodedPassword", "test2");
    user2 = userRepository.saveAndFlush(user2);

    // createdAt DESC 정렬 검증을 위해 저장 사이에 sleep
    n1 = notificationRepository.saveAndFlush(
        createNotification(user1, "testTitle1", "testContent1", Level.INFO, Status.UNREAD)
    );

    n2 = notificationRepository.saveAndFlush(
        createNotification(user1, "testTitle2", "testContent2", Level.INFO, Status.UNREAD)
    );

    n3 = notificationRepository.saveAndFlush(
        createNotification(user1, "testTitle3", "testContent3", Level.INFO, Status.READ)
    );

    n4 = notificationRepository.saveAndFlush(
        createNotification(user1, "testTitle4", "testContent4", Level.INFO, Status.UNREAD)
    );

    n5 = notificationRepository.saveAndFlush(
        createNotification(user2, "testTitle5", "testContent5", Level.INFO, Status.UNREAD)
    );

    userDto1 = userMapper.toDto(user1);
    customUserDetails1 = new CustomUserDetails(
        userDto1,
        "dummyPassword"
    );

    userDto2 = userMapper.toDto(user2);
    customUserDetails2 = new CustomUserDetails(
        userDto2,
        "dummyPassword"
    );

    Cache cache = cacheManager.getCache("notification:firstPage");
    if (cache != null) {
      cache.clear();
    }
  }

  @Test
  @DisplayName("알림 조회 성공 - UNDREAD 상태인 알림 3개를 최신순으로 조회")
  void getNotifications_success() throws Exception {
    // given
    int limit = 5;

    // when
    ResultActions resultActions = mockMvc.perform(
        get("/api/notifications")
            .with(user(customUserDetails1))
            .param("limit", String.valueOf(limit))
            .param("sortDirection", SortDirection.DESCENDING.toString())
            .param("sortBy", SortBy.CREATED_AT.getType())
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[1].length()").value(3))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(3))
        .andExpect(jsonPath("$.sortBy").value(SortBy.CREATED_AT.getType()))
        .andExpect(jsonPath("$.sortDirection").value(SortDirection.DESCENDING.toString()));
  }

  @Test
  @DisplayName("limit이 0 이하이면 400 반환")
  void getNotifications_invalidLimit() throws Exception {
    mockMvc.perform(
        get("/api/notifications")
            .with(user(customUserDetails1))
            .param("limit", "-1")
            .param("sortDirection", "DESCENDING")
            .param("sortBy", "createdAt")
    ).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("알림 삭제 성공 - 본인 알림을 삭제하면 204를 반환하고 UNREAD 상태를 READ로 바꾼다.")
  void deleteNotification_success() throws Exception {
    // given
    UUID notificationId = n1.getId();

    Notification deleted = notificationRepository.findById(notificationId).orElseThrow();
    assertThat(deleted.getStatus()).isEqualTo(Status.UNREAD);

    // when
    ResultActions resultActions = mockMvc.perform(
        delete("/api/notifications/{notificationId}", notificationId)
            .with(user(customUserDetails1))
            .with(csrf())
    );

    // then
    resultActions
        .andExpect(status().isNoContent());

    // 소프트 삭제니까 deleted의 값이 true로 바꿔야하 함
    deleted = notificationRepository.findById(notificationId).orElseThrow();
    assertThat(deleted.getStatus()).isEqualTo(Status.READ);
  }

  @Test
  @DisplayName("알림 삭제 실패 - 다른 유저의 알림을 삭제할 때 Forbidden 예외가 발생한다. ")
  void deleteNotification_forbidden() throws Exception {
    // given
    UUID notificationId = n5.getId();

    // when
    ResultActions resultActions = mockMvc.perform(
        delete("/api/notifications/{notificationId}", notificationId)
            .with(user(customUserDetails1))
            .with(csrf())
    );

    // then
    resultActions
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("알림 삭제 실패 - 없는 알림을 삭제할 때 NotFound 예외가 발생한다. ")
  void deleteNotification_notFound() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();

    // when
    ResultActions resultActions = mockMvc.perform(
        delete("/api/notifications/{notificationId}", notificationId)
            .with(user(customUserDetails1))
            .with(csrf())
    );

    // then
    resultActions
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Cursor가 없을 때, 첫 페이지 조회는 캐시가 적용되어, 같은 파라미터로 두 번 호출해도 Repository는 한 번만 호출된다")
  void getNotifications_firstPage_isCached() throws Exception {
    // given
    int limit = 5;

    // when
    ResultActions resultActions = mockMvc.perform(
        get("/api/notifications")
            .with(user(customUserDetails1))
            .param("limit", String.valueOf(limit))
            .param("sortDirection", SortDirection.DESCENDING.toString())
            .param("sortBy", SortBy.CREATED_AT.getType())
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    verify(notificationRepository, times(1)).searchNotifications(
        customUserDetails1.getUser().id(),
        null,
        null,
        limit,
        SortDirection.DESCENDING,
        SortBy.CREATED_AT
    );
  }

  private Notification createNotification(
      User user,
      String title,
      String content,
      Level level,
      Status status
  ) throws InterruptedException {
    Notification n = new Notification();
    n.setTitle(title);
    n.setContent(content);
    n.setUser(user);
    n.setLevel(level);
    n.setStatus(status);
    Thread.sleep(10);
    return n;
  }

  private User createUser(String email, String password, String name) {
    User user = new User(email, password, name);
    user.setRole(Role.USER);
    return user;
  }
}
