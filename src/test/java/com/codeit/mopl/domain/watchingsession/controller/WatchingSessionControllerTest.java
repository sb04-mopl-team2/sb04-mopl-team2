package com.codeit.mopl.domain.watchingsession.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mopl.domain.content.dto.response.contentSummary;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.domain.watchingsession.dto.CursorResponseWatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.codeit.mopl.domain.watchingsession.entity.UserSummary;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortBy;
import com.codeit.mopl.domain.watchingsession.entity.enums.SortDirection;
import com.codeit.mopl.domain.watchingsession.service.WatchingSessionService;
import com.codeit.mopl.exception.watchingsession.ErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.exception.watchingsession.ContentNotFoundException;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.CustomUserDetailsService;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.JwtRegistry;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(WatchingSessionController.class)
@Import({TestSecurityConfig.class})
public class WatchingSessionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private WatchingSessionService watchingSessionService;

  @Autowired
  private ObjectMapper om;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private UserMapper userMapper;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private JwtRegistry jwtRegistry;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JpaMetamodelMappingContext jpaMappingContext;

  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;


  @BeforeEach
  void setUp() {
    // setting this up for UserDetails
    String password = "password";
    UserDto mockUser = new UserDto(
        UUID.randomUUID(),
        LocalDateTime.now(),
        "test@test.com",
        "test",
        null,
        Role.USER,
        false);
    CustomUserDetails mockUserDetails = new CustomUserDetails(mockUser, password);

    given(customUserDetailsService.loadUserByUsername("test@test.com"))
        .willReturn(mockUserDetails);
  }

  /**
   * @GetMapping("/users/{watcherId}/watching-sessions") 관련 테스트들
   */

  @DisplayName("특정 사용자의 시청 목록 조회를 시도한다.")
  @Test
  void getWatchingSessionPerUserSuccess() throws Exception {
    // given
    UUID watcherId = UUID.randomUUID();
    UUID watchingSessionId = UUID.randomUUID();
    WatchingSessionDto watchingSessionDto = new WatchingSessionDto(
        watchingSessionId,
        LocalDateTime.now(),
        new UserSummary(
            watcherId,
            "test",
            null
        ),
        null
    );
    when(watchingSessionService.getByUserId(watcherId)).thenReturn(watchingSessionDto);

    // when
    ResultActions resultActions = mockMvc.perform(
        get("/api/users/" + watcherId +"/watching-sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE));

    // then
    resultActions.andExpect(jsonPath("$.id").value(watchingSessionId.toString()))
        .andExpect(jsonPath("$.userSummary.userId").value(watcherId.toString()))
        .andExpect(jsonPath("$.userSummary.name").value("test"))
        .andExpect(status().isOk());
  }

  @DisplayName("존재하지 않는 유저 아이디면 사용자별 시청 목록 조회는 실패한다")
  @Test
  void getWatchingSessionPerUserFailWhenInvalidUserId() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    given(watchingSessionService.getByUserId(any(UUID.class)))
        .willThrow(new UserNotFoundException(com.codeit.mopl.exception.user.ErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    // when & then
    ResultActions resultActions = mockMvc.perform(
            get("/api/users/" + userId +"/watching-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
    );
    resultActions.andExpect(status().isNotFound());
  }

  /**
   * @GetMapping("/contents/{contentId}/watching-sessions") 관련 테스트들
   */

  @DisplayName("특정 콘텐츠의 시청 세션 목록 조회를 시도한다")
  @Test
  void getWatchingSessionPerContentSuccess() throws Exception {
    String password = "password";
    UUID userId = UUID.randomUUID();
    UserDto mockUser = new UserDto(
        userId,
        LocalDateTime.now(),
        "test@test.com",
        "test",
        null,
        Role.USER,
        false);
    CustomUserDetails mockUserDetails = new CustomUserDetails(mockUser, password);

    Authentication auth = new UsernamePasswordAuthenticationToken(
        mockUserDetails,
        "password",
        mockUserDetails.getAuthorities()
    );

    // given
    UUID contentId = UUID.randomUUID();
    WatchingSessionDto watchingSessionDto = new WatchingSessionDto(
        UUID.randomUUID(),
        LocalDateTime.now(),
        new UserSummary(userId,"test",null),
        new contentSummary(contentId,null,null,null, null,null,null,null
        )
    );
    CursorResponseWatchingSessionDto responseWrapper = new CursorResponseWatchingSessionDto(
        List.of(watchingSessionDto),
        "nextCursor_123",
        UUID.randomUUID(),
        true,
        1L,
        SortBy.createdAt,
        SortDirection.ASCENDING
    );
    when(watchingSessionService.getWatchingSessions(
        any(UUID.class),
        eq(contentId),
        any(),
        any(),
        any(),
        anyInt(),
        any(),
        any()
    )).thenReturn(responseWrapper);


    // when & then
    ResultActions resultActions = mockMvc.perform(
        get("/api/contents/" + contentId +"/watching-sessions")
            .with(authentication(auth))
            .param("limit", "10")
            .param("sortDirection", "ASCENDING")
            .param("sortBy", "createdAt")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
    );
    resultActions.andExpect(status().isOk());

  }

  @DisplayName("잘못된 형태의 컨텐츠 아이디면 특정 콘텐츠의 시청 세션 목록 조회는 실패한다")
  @Test
  void getWatchingSessionPerContentFailWhenInvalidContentId() throws Exception {
    // given
    UUID contentId = UUID.randomUUID();

    // when & then
    ResultActions resultActions = mockMvc.perform(
        get("/api/contents/" + contentId +"/watching-sessions")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
    );
    resultActions.andExpect(status().isBadRequest());
  }

  @DisplayName("존재하지 않는 컨텐츠 아이디면 특정 콘텐츠의 시청 세션 목록 조회는 실패한다")
  @Test
  void getWatchingSessionPerContentFailWhenNonExistentContentId() throws Exception {
    String password = "password";
    UserDto mockUser = new UserDto(
        UUID.randomUUID(), // You can use a fixed UUID if you 'any()' it below
        LocalDateTime.now(),
        "test@test.com",
        "test",
        null,
        Role.USER,
        false);
    CustomUserDetails mockUserDetails = new CustomUserDetails(mockUser, password);

    Authentication auth = new UsernamePasswordAuthenticationToken(
        mockUserDetails,
        "password",
        mockUserDetails.getAuthorities()
    );

    // given
    UUID contentId = UUID.randomUUID();
    given(watchingSessionService.getWatchingSessions(
        any(UUID.class),
        eq(contentId),
        any(),
        any(),
        any(),
        anyInt(),
        any(),
        any()
        ))
        .willThrow(new ContentNotFoundException(
            ErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId))
        );

    // when & then
    ResultActions resultActions = mockMvc.perform(
        get("/api/contents/" + contentId +"/watching-sessions")
            .with(authentication(auth))
            .param("limit", "10")
            .param("sortDirection", "ASCENDING")
            .param("sortBy", "createdAt")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON_VALUE)
    );
    resultActions.andExpect(status().isNotFound());
  }
}
