package com.codeit.mopl.notification;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.entity.Notification;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.entity.Status;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.RepositoryNotificationRepository;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;
import static org.mockito.Mockito.*;

/**
 * NotificationService#getNotifications 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private RepositoryNotificationRepository notificationRepository;

  @Mock
  private NotificationMapper notificationMapper;

  @InjectMocks
  private NotificationService notificationService;

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
    this.sortBy = SortBy.createdAt;
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
    assertThat(result.sortBy()).isEqualTo(SortBy.createdAt);
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

    // limit+1 = 3개 생성
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
    // data는 limit 개수만 남아야 함
    assertThat(result.data()).hasSize(limit);

    // nextCursor / nextIdAfter 는 limit-1 인덱스의 createdAt / id
    assertThat(result.nextCursor()).isEqualTo(n2.getCreatedAt().toString());
    assertThat(result.hasNext()).isTrue();
    assertThat(result.totalCount()).isEqualTo(totalCount);
    assertThat(result.sortBy()).isEqualTo(sortBy);
    assertThat(result.sortDirection()).isEqualTo(sortDirection);

    verify(notificationRepository).countByUserIdAndStatus(userId, Status.UNREAD);
  }

  // ---- 테스트용 헬퍼 메소드들 ----

  private Notification createNotification(String title, LocalDateTime createdAt) {
    Notification notification = new Notification(UUID.randomUUID(), createdAt);
    // Notification 엔티티에 맞게 set 메소드 수정 필요, id를 설정할 수 없으므로 대용으로 title을 사용함
    notification.setTitle(title);
    notification.setStatus(Status.UNREAD);
    return notification;
  }

  private NotificationDto createDtoFrom(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getCreatedAt(),
        null,
        notification.getTitle(), // 실제 Dto 필드에 맞게 수정
        notification.getContent(),
        notification.getLevel()
    );
  }
}
