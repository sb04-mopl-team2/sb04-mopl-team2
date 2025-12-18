package com.codeit.mopl.domain.notification.controller;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationSearchRequest;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorResponseNotificationDto> getNotifications(
      @AuthenticationPrincipal CustomUserDetails user,
      @Valid NotificationSearchRequest request
  ) {
    log.info("[알림] 알림 조회 요청 시작, userId = {}", user.getUser().id());

    UUID userId = user.getUser().id();
    CursorResponseNotificationDto response = notificationService.getNotifications(userId,
        request.cursor(), request.idAfter(), request.limit(), request.sortDirection(), request.sortBy()
    );

    log.info("[알림] 알림 조회 요청 종료");
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> readNotification(
      @PathVariable("notificationId") UUID notificationId,
      @AuthenticationPrincipal CustomUserDetails user
  ){
    log.info("[알림] 알림 삭제 요청 시작, userId = {}, notificationId = {}", user.getUser().id(), notificationId);

    UUID userId = user.getUser().id();
    notificationService.deleteNotification(userId, notificationId);
    log.info("[알림] 알림 삭제 요청 종료");

    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }
}
