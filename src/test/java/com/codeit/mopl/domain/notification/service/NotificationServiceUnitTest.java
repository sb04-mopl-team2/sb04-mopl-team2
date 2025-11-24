package com.codeit.mopl.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.domain.notification.exception.NotificationForbidden;
import com.codeit.mopl.domain.notification.exception.NotificationNotFoundException;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.NotificationCreateEvent;
import com.codeit.mopl.sse.service.SseService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationServiceUnitTest {

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private NotificationMapper notificationMapper;

  @InjectMocks
  private NotificationService notificationService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private SseService sseService;

  private UUID userId;
  private String cursor;
  private UUID idAfter;
  private int limit;
  private SortDirection sortDirection;
  private SortBy sortBy;

  @BeforeEach
  void setUpParams() {
    this.userId = UUID.randomUUID();
    this.cursor = null;
    this.idAfter = null;
    this.limit = 3;
    this.sortDirection = SortDirection.DESCENDING;
    this.sortBy = SortBy.CREATED_AT;
  }

  @Test
  @DisplayName("알림이 하나도 없으면 빈 응답과 기본 sort 설정을 반환한다")
  void getNotifications_whenEmpty_shouldReturnEmptyResponse() {
    // given
    when(notificationRepository.searchNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    )).thenReturn(Collections.emptyList());

    // when
    CursorResponseNotificationDto result = notificationService.getNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    );

    // then
    assertThat(result.data()).isNull();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isZero();
    assertThat(result.sortBy()).isEqualTo(SortBy.CREATED_AT);
    assertThat(result.sortDirection()).isEqualTo(SortDirection.DESCENDING);

    verify(notificationRepository, never())
        .countByUserIdAndStatus(any(), any());
  }

  @Test
  @DisplayName("알림 개수가 limit 이하이면 hasNext=false, nextCursor=null 로 반환한다")
  void getNotifications_whenSizeLessOrEqualLimit_shouldNotHaveNext() {
    // given

    Notification n1 = createNotification(userId.toString(), LocalDateTime.now().minusMinutes(2));
    Notification n2 = createNotification(userId.toString(), LocalDateTime.now().minusMinutes(1));
    List<Notification> notifications = Arrays.asList(n1, n2);

    when(notificationRepository.searchNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    )).thenReturn(notifications);

    when(notificationMapper.toDto(n1)).thenReturn(createDtoFrom(n1));
    when(notificationMapper.toDto(n2)).thenReturn(createDtoFrom(n2));

    long totalCount = 2L;
    when(notificationRepository.countByUserIdAndStatus(userId, Status.UNREAD))
        .thenReturn(totalCount);

    // when
    CursorResponseNotificationDto result = notificationService.getNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    );

    // then
    assertThat(result.data()).hasSize(2);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.totalCount()).isEqualTo(totalCount);
    assertThat(result.sortBy()).isEqualTo(sortBy);
    assertThat(result.sortDirection()).isEqualTo(sortDirection);
  }

  @Test
  @DisplayName("알림 개수가 limit+1 이면 hasNext=true, nextCursor/nextIdAfter가 설정된다")
  void getNotifications_whenSizeGreaterThanLimit_shouldHaveNextAndSetCursor() {
    // given
    Notification n1 = createNotification("테스트 1", LocalDateTime.now().minusMinutes(4));
    Notification n2 = createNotification("테스트 2", LocalDateTime.now().minusMinutes(3));
    Notification n3 = createNotification("테스트 3", LocalDateTime.now().minusMinutes(2));
    Notification n4 = createNotification("테스트 4", LocalDateTime.now().minusMinutes(1));
    List<Notification> notifications = Arrays.asList(n4, n3, n2, n1);

    when(notificationRepository.searchNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    )).thenReturn(notifications);

    when(notificationMapper.toDto(n4)).thenReturn(createDtoFrom(n4));
    when(notificationMapper.toDto(n3)).thenReturn(createDtoFrom(n3));
    when(notificationMapper.toDto(n2)).thenReturn(createDtoFrom(n2));

    long totalCount = 10L;
    when(notificationRepository.countByUserIdAndStatus(userId, Status.UNREAD))
        .thenReturn(totalCount);

    // when
    CursorResponseNotificationDto result = notificationService.getNotifications(
        userId, cursor, idAfter, limit, sortDirection, sortBy
    );

    // then
    assertThat(result.data()).hasSize(limit);
    assertThat(result.nextCursor()).isEqualTo(n2.getCreatedAt().toString());
    assertThat(result.hasNext()).isTrue();
    assertThat(result.totalCount()).isEqualTo(totalCount);
    assertThat(result.sortBy()).isEqualTo(sortBy);
    assertThat(result.sortDirection()).isEqualTo(sortDirection);

    verify(notificationRepository).countByUserIdAndStatus(userId, Status.UNREAD);
  }

  @Test
  @DisplayName("정상적으로 알림을 읽음 처리한다")
  void deleteNotification_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    User user = mock(User.class);
    Notification notification = mock(Notification.class);

    when(notification.getUser()).thenReturn(user);
    when(user.getId()).thenReturn(userId);

    when(notificationRepository.findById(notificationId))
        .thenReturn(Optional.of(notification));


    // when
    notificationService.deleteNotification(userId, notificationId);

    // then
    verify(notification).setStatus(Status.READ);
    verify(notificationRepository).save(notification);
  }


  @Test
  @DisplayName("존재하지 않는 알림이면 NotificationNotFoundException 발생")
  void deleteNotification_notFound() {
    // given
    UUID notificationId = UUID.randomUUID();

    when(notificationRepository.findById(notificationId))
        .thenReturn(Optional.empty());

    // when
    Runnable act = () ->
        notificationService.deleteNotification(UUID.randomUUID(), notificationId);

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(NotificationNotFoundException.class);
  }

  @Test
  void deleteNotification_notOwner() {
    // given
    UUID ownerId = UUID.randomUUID();
    UUID attackerId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    User owner = mock(User.class);
    Notification notification = mock(Notification.class);

    when(notificationRepository.findById(notificationId))
        .thenReturn(Optional.of(notification));

    when(notification.getUser()).thenReturn(owner);
    when(owner.getId()).thenReturn(ownerId);

    // when
    Runnable act = () ->
        notificationService.deleteNotification(attackerId, notificationId);

    // then
    assertThatThrownBy(act::run)
        .isInstanceOf(NotificationForbidden.class);

    verify(notification, never()).setStatus(any(Status.class));
    verify(notificationRepository, never()).save(any(Notification.class));
  }

  @Test
  @DisplayName("알림 생성 메소드 테스트")
  void createNotification_shouldSaveEntityAndPublishEvent() {
    // given
    User user = new User();
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    String title = "테스트 제목";
    String content = "테스트 내용";
    Level level = Level.INFO;

    Notification notification = createNotification(title, LocalDateTime.now());
    NotificationDto notificationDto = createDtoFrom(notification);

    when(notificationMapper.toDto(any(Notification.class))).thenReturn(notificationDto);

    // when
    notificationService.createNotification(userId, title, content, level);

    // then
    verify(userRepository).findById(userId);
    verify(notificationRepository).save(any(Notification.class));
    verify(notificationMapper).toDto(any(Notification.class));

    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    Object published = eventCaptor.getValue();
    assertThat(published).isInstanceOf(NotificationCreateEvent.class);

    NotificationCreateEvent event = (NotificationCreateEvent) published;
    assertThat(event.notificationDto()).isEqualTo(notificationDto);
  }

  @Test
  @DisplayName("알림 송신 메소드 테스트")
  void sendNotification_shouldCallSseServiceSend() {
    // given
    UUID receiverId = UUID.randomUUID();
    String title = "테스트 제목";

    Notification notification = createNotification(title, LocalDateTime.now());
    NotificationDto notificationDto = createDtoFrom(notification, receiverId);

    // when
    notificationService.sendNotification(notificationDto);

    // then
    verify(sseService).send(eq(receiverId), eq("notification"), eq(notificationDto));
  }
  // ---- 테스트용 헬퍼 메소드들 ----
  private Notification createNotification(String title, LocalDateTime createdAt) {
    Notification notification = new Notification(UUID.randomUUID(), createdAt);
    notification.setTitle(title);
    notification.setStatus(Status.UNREAD);
    return notification;
  }

  private NotificationDto createDtoFrom(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getCreatedAt(),
        null,
        notification.getTitle(),
        notification.getContent(),
        notification.getLevel()
    );
  }

  private NotificationDto createDtoFrom(Notification notification, UUID receiverId) {
    return new NotificationDto(
        notification.getId(),
        notification.getCreatedAt(),
        receiverId,
        notification.getTitle(),
        notification.getContent(),
        notification.getLevel()
    );
  }
}
