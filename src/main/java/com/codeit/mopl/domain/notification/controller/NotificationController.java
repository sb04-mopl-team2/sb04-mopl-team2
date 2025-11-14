package com.codeit.mopl.domain.notification.controller;

import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationSearchRequest;
import com.codeit.mopl.domain.notification.entity.SortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.service.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
      @Validated NotificationSearchRequest request
  ) {
    // 서비스 호출

    //UUID userId = user.getId()
    // TODO 추후 AuthenticationPrincipal 기능이 구현되면 userId를 AuthenticationPrincipal 에서 가져오도록 변경하기
    UUID userId = UUID.randomUUID();

    CursorResponseNotificationDto response = notificationService.getNotifications(userId,
        request.cursor(), request.idAfter(), request.limit(), request.sortDirection(), request.sortBy()
    );
    return ResponseEntity.ok(response);
  }

}