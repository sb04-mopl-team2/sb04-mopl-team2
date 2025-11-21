package com.codeit.mopl.domain.review.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.notification.controller.NotificationController;
import com.codeit.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationDto;
import com.codeit.mopl.domain.notification.dto.NotificationSearchRequest;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.entity.NotificationSortBy;
import com.codeit.mopl.domain.notification.mapper.MapperUtils;
import com.codeit.mopl.domain.notification.mapper.NotificationMapper;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewCreateRequest;
import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewUpdateRequest;
import com.codeit.mopl.domain.review.entity.ReviewSortBy;
import com.codeit.mopl.domain.review.entity.SortDirection;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import com.codeit.mopl.domain.review.service.ReviewService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ReviewController.class)
@Import({TestSecurityConfig.class})
public class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper om;

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

  @MockitoBean
  private ReviewService reviewService;

  @MockitoBean
  private ReviewRepository reviewRepository;

  @MockitoBean
  private ContentRepository contentRepository;

  private CustomUserDetails customUserDetails;

  @BeforeEach
  void setUp() throws Exception {
    UserDto userDto = new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test@example.com","test",null, Role.USER, false);
    customUserDetails = new CustomUserDetails(
        userDto,
        "dummyPassword"
    );
  }

  @Test
  @DisplayName("리뷰 목록 조회 성공")
  void findReviews_success() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID contentId = UUID.randomUUID();
    int limit = 10;
    SortDirection sortDirection = SortDirection.DESCENDING;
    ReviewSortBy sortBy = ReviewSortBy.createdAt;

    ReviewDto reviewDto = new ReviewDto(
        UUID.randomUUID(),
        contentId,
        null,
        "리뷰 내용입니다.",
        4.5
    );

    CursorResponseReviewDto responseDto = new CursorResponseReviewDto(
        List.of(reviewDto),
        null,
        null,
        false,
        1L,
        sortBy.toString(),
        sortDirection.toString()
    );

    when(reviewService.findReviews(
        eq(contentId),
        any(),
        any(),
        eq(limit),
        eq(sortDirection),
        eq(sortBy)
    )).thenReturn(responseDto);

    // when
    ResultActions resultActions = mockMvc.perform(
        get("/api/reviews")
            .with(user(customUserDetails)) // 인증 유저
            .param("contentId", contentId.toString())
            .param("limit", String.valueOf(limit))
            .param("sortDirection", sortDirection.name()) // ex) "DESCENDING"
            .param("sortBy", sortBy.name())               // ex) "createdAt"
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].contentId").value(contentId.toString()))
        .andExpect(jsonPath("$.data[0].text").value("리뷰 내용입니다."))
        .andExpect(jsonPath("$.data[0].rating").value(4.5))
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(1));

    verify(reviewService).findReviews(
        eq(contentId),
        any(),
        any(),
        eq(limit),
        eq(sortDirection),
        eq(sortBy)
    );
  }

  @Test
  @DisplayName("리뷰 생성 성공")
  void createReview_success() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID contentId = UUID.randomUUID();

    String text = "리뷰 내용입니다.";
    double rating = 4.5;

    // 요청 DTO
    ReviewCreateRequest request = new ReviewCreateRequest(
        contentId,
        text,
        rating
    );

    // 응답 DTO (ReviewDto)
    ReviewDto responseDto = new ReviewDto(
        UUID.randomUUID(),     // reviewId
        contentId,
        null,
        text,
        rating
    );

    when(reviewService.createReview(
        eq(userId),
        eq(contentId),
        eq(text),
        eq(rating)
    )).thenReturn(responseDto);

    // when
    ResultActions resultActions = mockMvc.perform(
        post("/api/reviews")
            .with(user(customUserDetails))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(om.writeValueAsString(request))
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentId").value(contentId.toString()))
        .andExpect(jsonPath("$.text").value(text))
        .andExpect(jsonPath("$.rating").value(rating));

    verify(reviewService).createReview(userId, contentId, text, rating);
  }

  @Test
  @DisplayName("리뷰 수정 성공")
  void updateReview_success() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID reviewId = UUID.randomUUID();

    String newText = "수정된 리뷰 내용입니다.";
    double newRating = 4.5;

    String requestBody = om.writeValueAsString(
        new ReviewUpdateRequest(newText, newRating));

    when(reviewService.updateReview(eq(userId), eq(reviewId), eq(newText), eq(newRating)))
        .thenReturn(null);


    // when
    ResultActions resultActions = mockMvc.perform(
        patch("/api/reviews/{reviewId}", reviewId)
            .with(user(customUserDetails))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isOk());

    verify(reviewService).updateReview(userId, reviewId, newText, newRating);
  }

  @Test
  @DisplayName("리뷰 삭제 성공")
  void deleteReview_success() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID reviewId = UUID.randomUUID();

    // when
    ResultActions resultActions = mockMvc.perform(
        delete("/api/reviews/{reviewId}", reviewId)
            .with(user(customUserDetails))
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isNoContent());

    verify(reviewService).deleteReview(userId, reviewId);
  }

}
