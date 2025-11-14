package com.codeit.mopl.domain.notification.controller;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationSearchRequest;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<CursorResponseNotificationDto> getNotifications(
      @AuthenticationPrincipal UserDetails user,
      @Valid NotificationSearchRequest request
  ) {
    log.info("알림 조회 요청 실행");

    //UUID userId = user.getId()
    // TODO 추후 AuthenticationPrincipal 기능이 구현되면 userId를 AuthenticationPrincipal 에서 가져오도록 변경하기
    UUID userId = UUID.randomUUID();

    CursorResponseNotificationDto response = notificationService.getNotifications(userId,
        request.cursor(), request.idAfter(), request.limit(), request.sortDirection(), request.sortBy()
    );

    log.info("알림 조회 요청 종료");
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> readNotification(
      @PathVariable("notificationId") UUID notificationId,
      @AuthenticationPrincipal UserDetails user
  ){
    log.info("알림 삭제 요청 실행");

    //UUID userId = user.getId()
    // TODO 추후 AuthenticationPrincipal 기능이 구현되면 userId를 AuthenticationPrincipal 에서 가져오도록 변경하기
    UUID userId = UUID.randomUUID();

    notificationService.deleteNotification(userId, notificationId);
    log.info("알림 삭제 요청 종료");

    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }
}
