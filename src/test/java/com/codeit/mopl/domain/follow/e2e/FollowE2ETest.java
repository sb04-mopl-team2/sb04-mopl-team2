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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        FollowDto body = response.getBody();
        Assertions.assertNotNull(body);
        assertThat(body.id()).isNotNull();
        assertEquals(follower.id(), body.followerId());
        assertEquals(followee.id(), body.followeeId());

        UUID followId = body.id();
        Follow createdFollow = followRepository.findById(followId).orElse(null);
        assertThat(createdFollow).isNotNull();
        assertEquals(FollowStatus.PENDING, createdFollow.getFollowStatus());
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
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertEquals("FOLLOW_SELF_PROHIBITED", errorResponse.exceptionName());
        assertEquals("자기 자신을 팔로우할 수 없습니다.", errorResponse.message());
        assertEquals(follower.id().toString(), errorResponse.details().get("followerId"));
        assertEquals(follower.id().toString(), errorResponse.details().get("followeeId"));
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
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertEquals("FOLLOW_DUPLICATE", errorResponse.exceptionName());
        assertEquals("같은 사용자를 중복해서 팔로우할 수 없습니다.", errorResponse.message());
        assertEquals(follower.id().toString(), errorResponse.details().get("followerId"));
        assertEquals(followee.id().toString(), errorResponse.details().get("followeeId"));
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
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody());
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
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(false, response.getBody());
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
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertEquals("USER_NOT_FOUND", errorResponse.exceptionName());
        assertEquals("유저를 찾을 수 없습니다.", errorResponse.message());
        assertEquals(followeeId.toString(), errorResponse.details().get("userId"));
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
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Long followerCount = response.getBody();
        assertEquals(0L, followerCount);

        // 실제 값과 비교
        User followeeUser = userRepository.findById(followee.id()).orElse(null);
        assertThat(followeeUser).isNotNull();
        assertEquals(followerCount, followeeUser.getFollowerCount());
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
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertEquals("USER_NOT_FOUND", errorResponse.exceptionName());
        assertEquals("유저를 찾을 수 없습니다.", errorResponse.message());
        assertEquals(followeeId.toString(), errorResponse.details().get("userId"));
    }

    @Test
    @DisplayName("팔로우 삭제 성공")
    void deleteFollow_Success() {
        // given
        Follow follow = createFollow();

        // 팔로우 객체 상태 변경
        Follow createdFollow = followRepository.findById(follow.getId()).orElse(null);
        createdFollow.setFollowStatus(FollowStatus.CONFIRM);
        followRepository.saveAndFlush(createdFollow);

        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);
        
        // when
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/follows/" + follow.getId(),
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        Follow cancelledFollow = followRepository.findById(follow.getId()).orElse(null);
        assertThat(cancelledFollow).isNotNull();
        assertEquals(FollowStatus.CANCELLED, cancelledFollow.getFollowStatus());
    }

    @Test
    @DisplayName("팔로우 삭제 중단 - 이미 CANCELLED 상태인 팔로우 객체")
    void deleteFollow_Stop_isAlreadyCancelled() {
        // given
        Follow follow = createFollow();
        follow.setFollowStatus(FollowStatus.CONFIRM);
        followRepository.saveAndFlush(follow);
        
        // 첫 번째 팔로우 삭제 수행 -> follow 상태 CANCELLED로 변화
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);
        restTemplate.exchange(
                "/api/follows/" + follow.getId(),
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        // when
        // 두 번째 팔로우 삭제 수행
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/follows/" + follow.getId(),
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        // then
        // 멱등성
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        Follow afterSecondDeleteFollow = followRepository.findById(follow.getId()).orElse(null);
        assertEquals(FollowStatus.CANCELLED, afterSecondDeleteFollow.getFollowStatus());
    }

    @Test
    @DisplayName("팔로우 삭제 실패 - 존재하지 않는 팔로우 id")
    void deleteFollow_Failure_FollowNotFoundException() {
        // given
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);
        UUID followId = UUID.randomUUID();

        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/follows/" + followId,
                HttpMethod.DELETE,
                entity,
                ErrorResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertEquals("FOLLOW_NOT_FOUND", errorResponse.exceptionName());
        assertEquals("팔로우를 찾을 수 없습니다.", errorResponse.message());
        assertEquals(followId.toString(), errorResponse.details().get("followId"));
    }

    @Test
    @DisplayName("팔로우 삭제 실패 - PENDING, FAILED 상태의 팔로우 객체는 시스템에서 처리해야 함")
    void deleteFollow_Failure_FollowCannotDeleteWhileProcessingException() {
        // given
        Follow follow = createFollow();
        follow.setFollowStatus(FollowStatus.PENDING);
        followRepository.saveAndFlush(follow);

        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders);

        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/follows/" + follow.getId(),
                HttpMethod.DELETE,
                entity,
                ErrorResponse.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertEquals("FOLLOW_CANNOT_DELETE_WHILE_PROCESSING", errorResponse.exceptionName());
        assertEquals("시스템에서 처리 중인 팔로우 객체는 삭제할 수 없습니다.", errorResponse.message());
        assertEquals(follow.getId().toString(), errorResponse.details().get("followId"));
        assertEquals(follow.getFollowStatus().name(), errorResponse.details().get("followStatus"));
    }

    @Test
    @DisplayName("팔로우 삭제 실패 - 팔로워 본인이 아니면 팔로우 삭제 불가능")
    void deleteFollow_Failure_FollowDeleteForbiddenException() {
        // given
        // 팔로우 생성
        Follow follow = createFollow();
        follow.setFollowStatus(FollowStatus.CONFIRM);
        followRepository.saveAndFlush(follow);
        
        // Follower 로그아웃
        HttpEntity logoutEntity = new HttpEntity<>(defaultHeaders);
        restTemplate.postForEntity("/api/auth/sign-out", logoutEntity, Void.class);

        // 다른 사용자로 로그인
        HttpHeaders followeeLoginHeaders = new HttpHeaders();
        followeeLoginHeaders.add(HttpHeaders.COOKIE, defaultHeaders.getFirst(HttpHeaders.COOKIE));
        followeeLoginHeaders.add("X-XSRF-TOKEN", defaultHeaders.getFirst("X-XSRF-TOKEN"));
        SignInRequest followeeSignInRequest = new SignInRequest("followee@test.com", "password");
        HttpEntity followeeLoginEntity = getSignInRequest(followeeSignInRequest);
        ResponseEntity<JwtDto> loginJwtDto = restTemplate.postForEntity("/api/auth/sign-in", followeeLoginEntity, JwtDto.class);
        String followeeAccessToken = loginJwtDto.getBody().accessToken();
        followeeLoginHeaders.setBearerAuth(followeeAccessToken);

        HttpEntity<Void> entity = new HttpEntity<>(followeeLoginHeaders);

        // when
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                "/api/follows/" + follow.getId(),
                HttpMethod.DELETE,
                entity,
                ErrorResponse.class
        );

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertEquals("FOLLOW_DELETE_FORBIDDEN", errorResponse.exceptionName());
        assertEquals("해당 팔로우를 지울 권한이 없습니다.", errorResponse.message());
        assertEquals(follow.getId().toString(), errorResponse.details().get("followId"));
        assertEquals(follow.getFollower().getId().toString(), errorResponse.details().get("followerId"));
        UUID requesterId = loginJwtDto.getBody().userDto().id();
        assertEquals(requesterId.toString(), errorResponse.details().get("requesterId"));
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

    private Follow createFollow() {
        FollowRequest followRequest = new FollowRequest(followee.id());
        HttpEntity<FollowRequest> followHttpEntity = new HttpEntity<>(followRequest, defaultHeaders);
        ResponseEntity<FollowDto> followResponse = restTemplate.postForEntity("/api/follows", followHttpEntity, FollowDto.class);
        FollowDto follow = followResponse.getBody();
        return followRepository.findById(follow.id())
                .orElseThrow(() -> new AssertionError("팔로우 찾기 실패 - 해당 id를 가진 팔로우가 생성되지 않음"));
    }
}
