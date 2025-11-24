package com.codeit.mopl.domain.notification.controller;


import com.codeit.mopl.domain.auth.service.AuthService;
import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationSearchRequest;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.NotificationSortBy;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.notification.mapper.MapperUtils;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.codeit.mopl.sse.service.SseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({TestSecurityConfig.class})
public class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper om;

  @MockitoBean
  private NotificationService notificationService;

  @MockitoBean
  private NotificationRepository notificationRepository;

  @MockitoBean
  private NotificationMapper notificationMapper;

  @MockitoBean
  private SseService sseService;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private ApplicationEventPublisher eventPublisher;

  @MockitoBean
  private JpaMetamodelMappingContext jpaMappingContext;

  @MockitoBean
  private SseEmitterRegistry sseEmitterRegistry;

  @MockitoBean
  private MapperUtils mapperUtils;

  @MockitoBean
  private UserMapper userMapper;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private JwtRegistry jwtRegistry;

  @Test
  @DisplayName("알림 조회 성공")
  void getNotifications_success() throws Exception {
    // given
    UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@example.com","test",null, Role.USER, false);
    CustomUserDetails customUserDetails = new CustomUserDetails(
        userDto,
        "dummyPassword"
    );

    NotificationSearchRequest notificationSearchRequest =
        new NotificationSearchRequest(
            null,
            null,
            5,
            SortDirection.DESCENDING,
            NotificationSortBy.CREATED_AT
        );

    NotificationDto notificationDto1 = new NotificationDto(
        UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
        "test title1", "test content1", Level.INFO
    );
    NotificationDto notificationDto2 = new NotificationDto(
        UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
        "test title2", "test content2", Level.INFO
    );
    NotificationDto notificationDto3 = new NotificationDto(
        UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID(),
        "test title3", "test content3", Level.INFO
    );

    CursorResponseNotificationDto cursorResponseNotificationDto =
        new CursorResponseNotificationDto(
            List.of(notificationDto1, notificationDto2, notificationDto3),
            null,
            null,
            false,
            3L,
            NotificationSortBy.CREATED_AT,
            SortDirection.DESCENDING
        );

    when(notificationService.getNotifications(
        any(UUID.class),
        eq(notificationSearchRequest.cursor()),
        eq(notificationSearchRequest.idAfter()),
        eq(notificationSearchRequest.limit()),
        eq(notificationSearchRequest.sortDirection()),
        eq(notificationSearchRequest.notificationSortBy())
    )).thenReturn(cursorResponseNotificationDto);

    // when
    ResultActions resultActions = mockMvc.perform(
        get("/api/notifications")
            .with(user(customUserDetails))
            .param("limit", String.valueOf(notificationSearchRequest.limit()))
            .param("sortDirection", notificationSearchRequest.sortDirection().name())
            .param("notificationSortBy", notificationSearchRequest.notificationSortBy().name())
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(3))
        .andExpect(jsonPath("$.data[0].id").value(notificationDto1.id().toString()))
        .andExpect(jsonPath("$.data[1].id").value(notificationDto2.id().toString()))
        .andExpect(jsonPath("$.data[2].id").value(notificationDto3.id().toString()))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(3))
        .andExpect(jsonPath("$.notificationSortBy").value(NotificationSortBy.CREATED_AT.name()))
        .andExpect(jsonPath("$.sortDirection").value(SortDirection.DESCENDING.name()));

    verify(notificationService).getNotifications(
        any(UUID.class),
        eq(notificationSearchRequest.cursor()),
        eq(notificationSearchRequest.idAfter()),
        eq(notificationSearchRequest.limit()),
        eq(notificationSearchRequest.sortDirection()),
        eq(notificationSearchRequest.notificationSortBy())
    );
  }

  @Test
  @DisplayName("알림 삭제 성공")
  void deleteNotification_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    UserDto userDto = new UserDto(
        userId,
        LocalDateTime.now(),
        "test@example.com",
        "tester",
        null,
        Role.USER,
        false
    );

    CustomUserDetails customUserDetails =
        new CustomUserDetails(userDto, "dummyPassword");


    // when
    ResultActions resultActions = mockMvc.perform(
        delete("/api/notifications/{notificationId}", notificationId)
            .with(user(customUserDetails))
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isNoContent());

    verify(notificationService)
        .deleteNotification(eq(userId), eq(notificationId));
  }

}
