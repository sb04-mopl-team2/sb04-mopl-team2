package com.codeit.mopl.domain.follow.e2e;

import com.codeit.mopl.domain.auth.dto.JwtDto;
import com.codeit.mopl.domain.auth.dto.request.SignInRequest;
import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.entity.FollowStatus;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.notification.repository.NotificationRepository;
import com.codeit.mopl.domain.user.dto.request.UserCreateRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.global.ErrorResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FollowE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private HttpHeaders defaultHeaders = new HttpHeaders();
    private String followerAccessToken;

    private UserDto follower;
    private UserDto followee;

    @BeforeEach
    void setUp() {
        // CSRF 토큰 설정
        setCSRFToken();

        // follower, followee 회원가입
        String followerEmail = "follower@test.com";
        String followeeEmail = "followee@test.com";
        String password = "password";

        UserCreateRequest followerCreateRequest = new UserCreateRequest(
                "follower",
                followerEmail,
                password
        );

        UserCreateRequest followeeCreateRequest = new UserCreateRequest(
                "followee",
                followeeEmail,
                password
        );
        HttpEntity<UserCreateRequest> followerHttpEntity = getUserCreateRequestHttpEntity(followerCreateRequest);
        ResponseEntity<UserDto> followerDto = restTemplate.postForEntity("/api/users", followerHttpEntity, UserDto.class);

        HttpEntity<UserCreateRequest> followeeHttpEntity = getUserCreateRequestHttpEntity(followeeCreateRequest);
        ResponseEntity<UserDto> followeeDto = restTemplate.postForEntity("/api/users", followeeHttpEntity, UserDto.class);
        follower = followerDto.getBody();
        followee = followeeDto.getBody();

        // follower 로그인
        SignInRequest followerSignInRequest = new SignInRequest(followerEmail, password);
        HttpEntity followerLoginEntity = getSignInRequest(followerSignInRequest);
        ResponseEntity<JwtDto> loginJwtDto = restTemplate.postForEntity("/api/auth/sign-in", followerLoginEntity, JwtDto.class);
        followerAccessToken = loginJwtDto.getBody().accessToken();
        defaultHeaders.setBearerAuth(followerAccessToken);
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        followRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("팔로우 생성 성공")
    void createFollow_Success() {
        // given
        FollowRequest followRequest = new FollowRequest(followee.id());
        HttpEntity<FollowRequest> entity = new HttpEntity<>(followRequest, defaultHeaders);

        // when
        ResponseEntity<FollowDto> response = restTemplate.postForEntity("/api/follows", entity, FollowDto.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        FollowDto body = response.getBody();
        Assertions.assertNotNull(body);
        assertThat(body.id()).isNotNull();
        assertThat(body.followerId()).isEqualTo(follower.id());
        assertThat(body.followeeId()).isEqualTo(followee.id());

        UUID followId = body.id();
        Follow createdFollow = followRepository.findById(followId).orElse(null);
        assertThat(createdFollow).isNotNull();
        assertThat(createdFollow.getFollowStatus()).isEqualTo(FollowStatus.PENDING);
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 자기 자신은 팔로우 할 수 없음")
    void createFollow_Failure_FollowSelfProhibitedException() {
        // given
        FollowRequest followRequest = new FollowRequest(follower.id());
        HttpEntity<FollowRequest> entity = new HttpEntity<>(followRequest, defaultHeaders);

        // when
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/follows", entity, ErrorResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.exceptionName()).isEqualTo("FOLLOW_SELF_PROHIBITED");
        assertThat(errorResponse.message()).contains("자기 자신을 팔로우할 수 없습니다");
        assertThat(errorResponse.details().get("followerId")).isEqualTo(follower.id().toString());
        assertThat(errorResponse.details().get("followeeId")).isEqualTo(follower.id().toString());
    }

    @Test
    @DisplayName("팔로우 생성 실패 - 중복 팔로우할 수 없음")
    void createFollow_Failure_FollowDuplicateException() {
        // given
        FollowRequest followRequest = new FollowRequest(followee.id());
        HttpEntity<FollowRequest> entity = new HttpEntity<>(followRequest, defaultHeaders);

        // when
        // 첫 번째 팔로우 생성
        restTemplate.postForEntity("/api/follows", entity, FollowDto.class);

        // 두 번째 생성 시도
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity("/api/follows", entity, ErrorResponse.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.exceptionName()).isEqualTo("FOLLOW_DUPLICATE");
        assertThat(errorResponse.message()).contains("같은 사용자를 중복해서 팔로우할 수 없습니다.");
        assertThat(errorResponse.details().get("followerId")).isEqualTo(follower.id().toString());
        assertThat(errorResponse.details().get("followeeId")).isEqualTo(followee.id().toString());
    }

    @Test
    @DisplayName("팔로우 여부 조회 성공 - 팔로우 된 상태면 true 반환")
    void isFollowedByMe_Success_isFollowedTrue() {
        // given
        // 팔로우 생성
        FollowRequest followRequest = new FollowRequest(followee.id());
        HttpEntity<FollowRequest> followHttpEntity = new HttpEntity<>(followRequest, defaultHeaders);
        restTemplate.postForEntity("/api/follows", followHttpEntity, FollowDto.class);

        // 팔로우 여부 조회용 HttpEntity 생성
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);

        // when
        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/api/follows/followed-by-me?followeeId=" + followee.id(),
                HttpMethod.GET,
                entity,
                Boolean.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    @DisplayName("팔로우 여부 조회 성공 - 팔로우 안된 상태면 false 반환")
    void isFollowedByMe_Success_isFollowedFalse() {
        // given
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);

        // when
        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/api/follows/followed-by-me?followeeId=" + followee.id(),
                HttpMethod.GET,
                entity,
                Boolean.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    @DisplayName("팔로우 여부 조회 실패 - 존재하지 않는 유저")
    void isFollowedByMe_Failure_UserNotFoundException() {
        // given
        UUID followeeId = UUID.randomUUID();
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);

        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/follows/followed-by-me?followeeId=" + followeeId,
                HttpMethod.GET,
                entity,
                ErrorResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.exceptionName()).isEqualTo("USER_NOT_FOUND");
        assertThat(errorResponse.message()).contains("유저를 찾을 수 없습니다.");
        assertThat(errorResponse.details().get("userId")).isEqualTo(followeeId.toString());
    }

    @Test
    @DisplayName("팔로워 수 조회 성공")
    void getFollowerCount_Success() {
        // given
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);

        // when
        ResponseEntity<Long> response = restTemplate.exchange(
                "/api/follows/count?followeeId=" + followee.id(),
                HttpMethod.GET,
                entity,
                Long.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Long followerCount = response.getBody();
        assertThat(followerCount).isEqualTo(0L);
        
        // 실제 값과 비교
        User followeeUser = userRepository.findById(followee.id()).orElse(null);
        assertThat(followeeUser).isNotNull();
        assertThat(followeeUser.getFollowerCount()).isEqualTo(followerCount);
    }

    @Test
    @DisplayName("팔로워 수 조회 실패 - 존재하지 않는 유저")
    void getFollowerCount_Failure_UserNotFoundException() {
        // given
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);
        UUID followeeId = UUID.randomUUID();

        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/follows/count?followeeId=" + followeeId,
                HttpMethod.GET,
                entity,
                ErrorResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.exceptionName()).isEqualTo("USER_NOT_FOUND");
        assertThat(errorResponse.message()).contains("유저를 찾을 수 없습니다.");
        assertThat(errorResponse.details().get("userId")).isEqualTo(followeeId.toString());
    }

    private void setCSRFToken() {
        ResponseEntity<String> csrfInitResponse =
                restTemplate.getForEntity("/api/auth/csrf-token", String.class);

        List<String> setCookieHeaders = csrfInitResponse.getHeaders().get(HttpHeaders.SET_COOKIE);

        String xsrfCookie = setCookieHeaders.stream()
                .filter(cookie -> cookie.startsWith("XSRF-TOKEN"))
                .findFirst()
                .orElseThrow();

        String tokenValue = xsrfCookie.split(";")[0].split("=")[1];
        defaultHeaders = new HttpHeaders();
        defaultHeaders.setContentType(MediaType.APPLICATION_JSON);

        defaultHeaders.add(HttpHeaders.COOKIE, xsrfCookie);
        defaultHeaders.add("X-XSRF-TOKEN", tokenValue);
    }

    private HttpEntity<UserCreateRequest> getUserCreateRequestHttpEntity(UserCreateRequest userCreateRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.add(HttpHeaders.COOKIE, defaultHeaders.getFirst(HttpHeaders.COOKIE));
        headers.add("X-XSRF-TOKEN", defaultHeaders.getFirst("X-XSRF-TOKEN"));

        return new HttpEntity<>(userCreateRequest, headers);
    }

    private HttpEntity<MultiValueMap<String, String>> getSignInRequest(SignInRequest signInRequest) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", signInRequest.username());
        params.add("password", signInRequest.password());

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        loginHeaders.add(HttpHeaders.COOKIE, defaultHeaders.getFirst(HttpHeaders.COOKIE));
        loginHeaders.add("X-XSRF-TOKEN", defaultHeaders.getFirst("X-XSRF-TOKEN"));

        return new HttpEntity<>(params, loginHeaders);
    }
}
