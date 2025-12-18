package com.codeit.mopl.domain.review;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.repository.ContentRepository;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.review.dto.ReviewCreateRequest;
import com.codeit.mopl.domain.review.dto.ReviewUpdateRequest;
import com.codeit.mopl.domain.review.entity.Review;
import com.codeit.mopl.domain.review.repository.ReviewRepository;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;

    private Content content1;
    private Content content2;

    private Review r1;
    private Review r2;
    private Review r3;

    private CustomUserDetails customUserDetails1;
    private CustomUserDetails customUserDetails2;

    private UserDto userDto1;
    private UserDto userDto2;


    @BeforeEach
    void setUp() throws Exception {
        // 혹시 모를 이전 데이터 정리 (FK 고려해서 순서: review → notification → user, content)
        reviewRepository.deleteAll();
        notificationRepository.deleteAll();
        contentRepository.deleteAll();
        userRepository.deleteAll();

        // 유저 생성/저장
        user1 = createUser("test@example.com", "encodedPassword", "test");
        user1 = userRepository.saveAndFlush(user1);

        user2 = createUser("test2@example.com", "encodedPassword", "test2");
        user2 = userRepository.saveAndFlush(user2);

        // 콘텐츠 생성/저장
        content1 = createContent("컨텐츠1", "설명1", ContentType.TV);
        content1 = contentRepository.saveAndFlush(content1);

        content2 = createContent("컨텐츠2", "설명2", ContentType.MOVIE);
        content2 = contentRepository.saveAndFlush(content2);

        // 리뷰 생성/저장
        // r1: user1, content1, 삭제 안 됨
        r1 = createReview(user1, content1, 4.0, false);
        r1 = reviewRepository.saveAndFlush(r1);
        Thread.sleep(5);

        // r2: user2, content1, 삭제됨 (조회 결과에서 빠져야 함)
        r2 = createReview(user2, content1, 5.0, true);
        r2 = reviewRepository.saveAndFlush(r2);
        Thread.sleep(5);

        // r3: user1, content2, 삭제 안 됨 (다른 콘텐츠라서 빠져야 함)
        r3 = createReview(user1, content2, 3.0, false);
        r3 = reviewRepository.saveAndFlush(r3);

        // 인증용 CustomUserDetails 세팅
        userDto1 = userMapper.toDto(user1);
        customUserDetails1 = new CustomUserDetails(userDto1, "dummyPassword");

        userDto2 = userMapper.toDto(user2);
        customUserDetails2 = new CustomUserDetails(userDto2, "dummyPassword");
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - 특정 콘텐츠의 삭제되지 않은 리뷰를 최신순으로 조회")
    void findReviews_success() throws Exception {
        // given
        UUID contentId = content1.getId();
        int limit = 10;
        SortDirection sortDirection = SortDirection.DESCENDING;
        SortBy sortBy = SortBy.CREATED_AT;

        // when
        ResultActions resultActions = mockMvc.perform(
                get("/api/reviews")
                        .with(user(customUserDetails1))
                        .param("contentId", contentId.toString())
                        .param("limit", String.valueOf(limit))
                        .param("sortDirection", sortDirection.name()) // "DESCENDING"
                        .param("sortBy", sortBy.name())               // "createdAt"
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(r1.getId().toString()))
                .andExpect(jsonPath("$.data[0].contentId").value(contentId.toString()))
                .andExpect(jsonPath("$.data[0].rating").value(r1.getRating()))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.sortBy").value(sortBy.name()))
                .andExpect(jsonPath("$.sortDirection").value(sortDirection.name()));
    }

    @Test
    @DisplayName("리뷰 목록 조회 실패 - 요청 값이 유효하지 않을 떄")
    void findReviews_invalidLimit() throws Exception {
        // given
        UUID contentId = content1.getId();
        int limit = 0;
        SortDirection sortDirection = SortDirection.DESCENDING;
        SortBy sortBy = SortBy.CREATED_AT;

        // when & then
        mockMvc.perform(
                get("/api/reviews")
                        .with(user(customUserDetails1))
                        .param("contentId", contentId.toString())
                        .param("limit", String.valueOf(limit))
                        .param("sortDirection", sortDirection.name()) // "DESCENDING"
                        .param("sortBy", sortBy.name())               // "createdAt"
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest());
        ;

    }

    @Test
    @DisplayName("리뷰 생성 성공 - 인증된 사용자가 유효한 요청을 보내면 리뷰가 생성된다")
    void createReview_success() throws Exception {
        // given
        UUID contentId = content2.getId();
        double rating = 4.5;
        String text = "리뷰 내용입니다.";

        ReviewCreateRequest request = new ReviewCreateRequest(
                contentId,
                text,
                rating
        );

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/reviews")
                        .with(user(customUserDetails2))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.contentId").value(contentId.toString()))
                .andExpect(jsonPath("$.text").value(text))
                .andExpect(jsonPath("$.rating").value(rating));

        List<Review> all = reviewRepository.findAll();
        assertThat(all).hasSize(4);

        assertThat(all).anySatisfy(review -> {
            assertThat(review.getUser().getId()).isEqualTo(user2.getId());
            assertThat(review.getContent().getId()).isEqualTo(contentId);
            assertThat(review.getText()).isEqualTo(text);
            assertThat(review.getRating()).isEqualTo(rating);
            assertThat(review.getIsDeleted()).isFalse();
        });
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 리뷰를 중복 생성하려고 하면 예외가 발생")
    void createReview_duplicated() throws Exception {
        // given
        UUID contentId = content1.getId();
        double rating = 4.5;
        String text = "리뷰 내용입니다.";

        ReviewCreateRequest request = new ReviewCreateRequest(
                contentId,
                text,
                rating
        );

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/api/reviews")
                        .with(user(customUserDetails1))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("리뷰 수정 성공 - 본인 리뷰를 수정하면 200과 수정된 리뷰가 반환된다")
    void updateReview_success() throws Exception {
        // given
        UUID reviewId = r1.getId();

        ReviewUpdateRequest request = new ReviewUpdateRequest(
                "수정된 리뷰입니다.",
                4.8
        );

        Review before = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(before.getText()).isEqualTo("sample");
        assertThat(before.getRating()).isEqualTo(4.0);

        // when
        ResultActions resultActions = mockMvc.perform(
                patch("/api/reviews/{reviewId}", reviewId)
                        .with(user(customUserDetails1))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId.toString()))
                .andExpect(jsonPath("$.text").value("수정된 리뷰입니다."))
                .andExpect(jsonPath("$.rating").value(4.8));

        Review after = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(after.getText()).isEqualTo("수정된 리뷰입니다.");
        assertThat(after.getRating()).isEqualTo(4.8);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 다른 유저의 리뷰를 수정하려고 할 때 예외가 발생")
    void updateReview_forbidden() throws Exception {
        // given
        UUID reviewId = r1.getId();

        ReviewUpdateRequest request = new ReviewUpdateRequest(
                "수정된 리뷰입니다.",
                4.8
        );

        // when & then
        mockMvc.perform(
                patch("/api/reviews/{reviewId}", reviewId)
                        .with(user(customUserDetails2))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 리뷰가 없어서")
    void updateReview_notFound() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();

        ReviewUpdateRequest request = new ReviewUpdateRequest(
                "수정된 리뷰입니다.",
                4.8
        );

        // when & then
        mockMvc.perform(
                patch("/api/reviews/{reviewId}", reviewId)
                        .with(user(customUserDetails2))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("리뷰 삭제 성공 - 본인 리뷰를 삭제하면 204 반환되고 soft delete 상태가 된다")
    void deleteReview_success() throws Exception {
        // given
        UUID reviewId = r1.getId();

        Review before = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(before.getIsDeleted()).isFalse();

        // when
        ResultActions resultActions = mockMvc.perform(
                delete("/api/reviews/{reviewId}", reviewId)
                        .with(user(customUserDetails1))
                        .with(csrf())
        );

        // then
        resultActions.andExpect(status().isNoContent());

        // DB soft delete 검증
        Review after = reviewRepository.findById(reviewId).orElseThrow();
        assertThat(after.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 다른 사용자가 리뷰를 삭제하려고 하면 Forbidden 예외가 발생한다")
    void deleteReview_forbidden() throws Exception {
        // given
        UUID reviewId = r1.getId();

        // when
        ResultActions resultActions = mockMvc.perform(
                delete("/api/reviews/{reviewId}", reviewId)
                        .with(user(customUserDetails2))
                        .with(csrf())
        );

        // then
        resultActions
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 다른 사용자가 리뷰를 삭제하려고 하면 Forbidden 예외가 발생한다")
    void deleteReview_notFound() throws Exception {
        // given
        UUID reviewId = UUID.randomUUID();

        // when
        ResultActions resultActions = mockMvc.perform(
                delete("/api/reviews/{reviewId}", reviewId)
                        .with(user(customUserDetails2))
                        .with(csrf())
        );

        // then
        resultActions
                .andExpect(status().isNotFound());
    }

    private User createUser(String email, String password, String name) {
        return new User(email, password, name);
    }

    private Content createContent(String title, String description, ContentType contentType) {
        Content content = new Content();
        content.setTitle(title);
        content.setDescription(description);
        content.setContentType(contentType);
        return content;
    }

    private Review createReview(User user, Content content, double rating, boolean isDeleted) {
        Review review = new Review();
        review.setUser(user);
        review.setContent(content);
        review.setText("sample");
        review.setRating(rating);
        review.setIsDeleted(isDeleted);
        return review;
    }
}
