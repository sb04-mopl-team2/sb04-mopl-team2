package com.codeit.mopl.domain.review.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mopl.domain.base.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.review.dto.CursorResponseReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewCreateRequest;
import com.codeit.mopl.domain.review.dto.ReviewDto;
import com.codeit.mopl.domain.review.dto.ReviewUpdateRequest;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import com.codeit.mopl.domain.review.service.ReviewService;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.review.ReviewDuplicateException;
import com.codeit.mopl.exception.review.ReviewErrorCode;
import com.codeit.mopl.exception.review.ReviewForbiddenException;
import com.codeit.mopl.exception.review.ReviewNotFoundException;
import com.codeit.mopl.oauth.service.OAuth2UserService;
import com.codeit.mopl.security.CustomUserDetails;
import com.codeit.mopl.security.config.TestSecurityConfig;
import com.codeit.mopl.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.codeit.mopl.security.jwt.handler.OAuth2UserSuccessHandler;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.registry.JwtRegistry;
import com.codeit.mopl.sse.repository.SseEmitterRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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

  @MockitoBean
  private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

  @MockitoBean
  private OAuth2UserService oAuth2UserService;

  @MockitoBean
  private OAuth2UserSuccessHandler oAuth2UserSuccessHandler;

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  private CustomUserDetails customUserDetails;

  @BeforeEach
  void setUp() throws Exception {
    UserDto userDto = new UserDto(UUID.randomUUID(), Instant.now(), "test@example.com","test",null, Role.USER, false);
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
    SortBy sortBy = SortBy.CREATED_AT;

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

  @Test
  @DisplayName("리뷰 삭제 실패 - 권한이 없어서")
  void deleteReview_forbidden() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID reviewId = UUID.randomUUID();

    doThrow(new ReviewForbiddenException(
        ReviewErrorCode.REVIEW_FORBIDDEN,
        Map.of("reviewId", reviewId)
    )).when(reviewService).deleteReview(userId, reviewId);

    // when
    ResultActions resultActions = mockMvc.perform(
        delete("/api/reviews/{reviewId}", reviewId)
            .with(user(customUserDetails))
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.exceptionName").value(ReviewErrorCode.REVIEW_FORBIDDEN.name()))
        .andExpect(jsonPath("$.message").value(ReviewErrorCode.REVIEW_FORBIDDEN.getMessage()))
        .andExpect(jsonPath("$.details.reviewId").value(reviewId.toString()));
  }

  @Test
  @DisplayName("리뷰 삭제 실패 - 리뷰가 없어서")
  void deleteReview_notFound() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID reviewId = UUID.randomUUID();

    doThrow(new ReviewNotFoundException(
        ReviewErrorCode.REVIEW_NOT_FOUND,
        Map.of("reviewId", reviewId)
    )).when(reviewService).deleteReview(userId, reviewId);

    // when
    ResultActions resultActions = mockMvc.perform(
        delete("/api/reviews/{reviewId}", reviewId)
            .with(user(customUserDetails))
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON)
    );

    // then
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionName").value(ReviewErrorCode.REVIEW_NOT_FOUND.name()))
        .andExpect(jsonPath("$.message").value(ReviewErrorCode.REVIEW_NOT_FOUND.getMessage()))
        .andExpect(jsonPath("$.details.reviewId").value(reviewId.toString()));
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 권한이 없어서")
  void updateReview_forbidden() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID reviewId = UUID.randomUUID();

    String newText = "수정된 리뷰 내용입니다.";
    double newRating = 4.5;

    String requestBody = om.writeValueAsString(
        new ReviewUpdateRequest(newText, newRating));

    doThrow(new ReviewForbiddenException(
        ReviewErrorCode.REVIEW_FORBIDDEN,
        Map.of("reviewId", reviewId)
    )).when(reviewService).updateReview(userId, reviewId, newText, newRating);

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
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.exceptionName").value(ReviewErrorCode.REVIEW_FORBIDDEN.name()))
        .andExpect(jsonPath("$.message").value(ReviewErrorCode.REVIEW_FORBIDDEN.getMessage()))
        .andExpect(jsonPath("$.details.reviewId").value(reviewId.toString()));
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 리뷰가 없어서")
  void updateReview_notFound() throws Exception {
    // given
    UUID userId = customUserDetails.getUser().id();
    UUID reviewId = UUID.randomUUID();

    String newText = "수정된 리뷰 내용입니다.";
    double newRating = 4.5;

    String requestBody = om.writeValueAsString(
        new ReviewUpdateRequest(newText, newRating));

    doThrow(new ReviewNotFoundException(
        ReviewErrorCode.REVIEW_NOT_FOUND,
        Map.of("reviewId", reviewId)
    )).when(reviewService).updateReview(userId, reviewId, newText, newRating);

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
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionName").value(ReviewErrorCode.REVIEW_NOT_FOUND.name()))
        .andExpect(jsonPath("$.message").value(ReviewErrorCode.REVIEW_NOT_FOUND.getMessage()))
        .andExpect(jsonPath("$.details.reviewId").value(reviewId.toString()));
  }

  @Test
  @DisplayName("리뷰 생성 실패 - 이미 리뷰가 있어서")
  void createReview_duplicated() throws Exception {
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

    doThrow(new ReviewDuplicateException(
        ReviewErrorCode.REVIEW_DUPLICATED,
        Map.of("userId", userId)
    )).when(reviewService).createReview(userId, contentId, text, rating);

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
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.exceptionName").value(ReviewErrorCode.REVIEW_DUPLICATED.name()))
        .andExpect(jsonPath("$.message").value(ReviewErrorCode.REVIEW_DUPLICATED.getMessage()))
        .andExpect(jsonPath("$.details.userId").value(userId.toString()));
  }
}
