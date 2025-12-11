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

        // 헤더 생성: AccessToken, CSRF
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(followerAccessToken);

        headers.add(HttpHeaders.COOKIE, defaultHeaders.getFirst(HttpHeaders.COOKIE));
        headers.add("X-XSRF-TOKEN", defaultHeaders.getFirst("X-XSRF-TOKEN"));

        HttpEntity<FollowRequest> entity = new HttpEntity<>(followRequest, headers);

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

        // 헤더 생성: AccessToken, CSRF
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(followerAccessToken);

        headers.add(HttpHeaders.COOKIE, defaultHeaders.getFirst(HttpHeaders.COOKIE));
        headers.add("X-XSRF-TOKEN", defaultHeaders.getFirst("X-XSRF-TOKEN"));

        HttpEntity<FollowRequest> entity = new HttpEntity<>(followRequest, headers);

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

        // 헤더 생성: AccessToken, CSRF
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(followerAccessToken);

        headers.add(HttpHeaders.COOKIE, defaultHeaders.getFirst(HttpHeaders.COOKIE));
        headers.add("X-XSRF-TOKEN", defaultHeaders.getFirst("X-XSRF-TOKEN"));

        HttpEntity<FollowRequest> entity = new HttpEntity<>(followRequest, headers);

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
