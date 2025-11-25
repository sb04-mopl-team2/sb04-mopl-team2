package com.codeit.mopl.domain.user.e2e;

import com.codeit.mopl.domain.auth.dto.JwtDto;
import com.codeit.mopl.domain.auth.dto.request.SignInRequest;
import com.codeit.mopl.domain.user.dto.request.*;
import com.codeit.mopl.domain.user.dto.response.CursorResponseUserDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.fixture.UserFixture;
import com.codeit.mopl.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserE2ETest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    private HttpHeaders defaultHeaders = new HttpHeaders();

    @BeforeEach
    void setUp() {
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(ops);
        if (!userRepository.existsByEmail("admin@google.com")){
            User admin = new User("admin@google.com",passwordEncoder.encode("asdf1234!"),"admin");
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }
        var httpClient = org.apache.hc.client5.http.impl.classic.HttpClients.createDefault();
        var factory = new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(httpClient);
        rest.getRestTemplate().setRequestFactory(factory);

        ResponseEntity<String> csrfInitResponse =
                rest.getForEntity("/api/auth/csrf-token", String.class);

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

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @DisplayName("회원가입에 성공한다")
    @Test
    void createUser() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());
    }

    @DisplayName("회원가입 이후 로그인에 성공한다")
    @Test
    void createUserAndLogin() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());

        SignInRequest signInRequest = new SignInRequest("test@test.com","password");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());
    }

    @DisplayName("회원가입 및 로그인 이후 토큰 재발급에 성공한다")
    @Test
    void createUserAndLoginWithRefreshToken() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());

        SignInRequest signInRequest = new SignInRequest("test@test.com","password");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpEntity refreshHttpEntity = new HttpEntity<>(getHttpHeaders(loginJwtDto));
        ResponseEntity<JwtDto> refresh = rest.postForEntity("/api/auth/refresh", refreshHttpEntity, JwtDto.class);

        assertEquals(HttpStatus.OK, refresh.getStatusCode());
    }

    @DisplayName("회원가입 및 로그인 이후 로그아웃까지 성공한다")
    @Test
    void createUserAndLogInAndLogOut() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());

        SignInRequest signInRequest = new SignInRequest("test@test.com","password");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpEntity refreshHttpEntity = new HttpEntity<>(getHttpHeaders(loginJwtDto));
        ResponseEntity logout = rest.postForEntity("/api/auth/sign-out", refreshHttpEntity, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, logout.getStatusCode());
    }

    @DisplayName("회원가입 및 로그인 이후 본인의 프로필을 확인한다")
    @Test
    void createUserAndLoginAndCheckProfile() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());

        SignInRequest signInRequest = new SignInRequest("test@test.com","password");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpHeaders headers = getHttpHeaders(loginJwtDto);
        HttpEntity authEntity = new HttpEntity<>(headers);
        UUID userId = loginJwtDto.getBody().userDto().id();

        ResponseEntity<UserDto> findResponse = rest.exchange(
                "/api/users/" + userId,
                HttpMethod.GET,
                authEntity,
                UserDto.class
        );
        assertEquals(HttpStatus.OK, findResponse.getStatusCode());
        assertEquals(userId, findResponse.getBody().id());
        assertEquals("test@test.com", findResponse.getBody().email());
    }

    @DisplayName("회원가입 및 로그인 이후 비밀번호를 변경한다")
    @Test
    void createUserAndLoginAndUpdatePassword() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());

        SignInRequest signInRequest = new SignInRequest("test@test.com","password");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpHeaders headers = getHttpHeaders(loginJwtDto);
        ChangePasswordRequest changePasswordRequest =
                new ChangePasswordRequest("changePassword");
        HttpEntity<ChangePasswordRequest> authEntity = new HttpEntity<>(changePasswordRequest,headers);
        UUID userId = loginJwtDto.getBody().userDto().id();

        ResponseEntity<Void> changePasswordResponse = rest.exchange(
                "/api/users/" + userId + "/password",
                HttpMethod.PATCH,
                authEntity,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, changePasswordResponse.getStatusCode());

        SignInRequest changeSignInRequest = new SignInRequest("test@test.com","changePassword");
        HttpEntity changeLoginHttpEntity = getSignInRequest(changeSignInRequest);
        ResponseEntity<JwtDto> changeLoginJwtDto = rest.postForEntity("/api/auth/sign-in", changeLoginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",changeLoginJwtDto.getBody().userDto().email());
        assertNotNull(changeLoginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, changeLoginJwtDto.getStatusCode());
    }

    @DisplayName("회원가입 및 로그인 이후 프로필 업데이트에 성공한다")
    @Test
    void createUserAndLoginAndUpdateProfile() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());

        SignInRequest signInRequest = new SignInRequest("test@test.com","password");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpHeaders headers = getHttpHeaders(loginJwtDto);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        UserUpdateRequest userUpdateRequest =
                new UserUpdateRequest("update");
        MultiValueMap<String, Object> updateRequest = new LinkedMultiValueMap<>();
        updateRequest.add("request", userUpdateRequest);
        updateRequest.add("image", null);
        HttpEntity authEntity = new HttpEntity<>(updateRequest,headers);
        UUID userId = loginJwtDto.getBody().userDto().id();

        ResponseEntity<UserDto> updateProfileResponse = rest.exchange(
                "/api/users/" + userId,
                HttpMethod.PATCH,
                authEntity,
                UserDto.class
        );

        assertEquals(HttpStatus.OK, updateProfileResponse.getStatusCode());
    }

    @DisplayName("서버 시작 시 자동으로 생성되는 어드민 계정으로 로그인에 성공한다")
    @Test
    void LoginAdmin() {
        SignInRequest signInRequest = new SignInRequest("admin@google.com","asdf1234!");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("admin@google.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());
        assertEquals(Role.ADMIN, loginJwtDto.getBody().userDto().role());
    }

    @DisplayName("일반 계정 회원가입 이후 어드민 계정으로 로그인하여 일반계정을 잠금 처리한다" +
            "잠긴 일반 계정은 로그인에 실패한다")
    @Test
    void CreateUserAndAdminLoginAndLockUser() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());
        assertEquals(false, createdUser.getBody().locked());
        assertEquals(Role.USER, createdUser.getBody().role());

        SignInRequest signInRequest = new SignInRequest("admin@google.com","asdf1234!");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("admin@google.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());
        assertEquals(Role.ADMIN, loginJwtDto.getBody().userDto().role());

        HttpHeaders headers = getHttpHeaders(loginJwtDto);
        UserLockUpdateRequest userLockUpdateRequest =
                new UserLockUpdateRequest(true);
        HttpEntity<UserLockUpdateRequest> authEntity = new HttpEntity<>(userLockUpdateRequest,headers);
        UUID userId = createdUser.getBody().id();

        ResponseEntity<Void> changePasswordResponse = rest.exchange(
                "/api/users/" + userId + "/locked",
                HttpMethod.PATCH,
                authEntity,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, changePasswordResponse.getStatusCode());
        User findUser = userRepository.findById(userId).orElse(null);
        assertNotNull(findUser);
        assertTrue(findUser.isLocked());

        SignInRequest lockUserSignInRequest = new SignInRequest("test@test.com", "password");
        HttpEntity changeLoginHttpEntity = getSignInRequest(lockUserSignInRequest);
        ResponseEntity<JwtDto> lockUserJwtDto = rest.postForEntity("/api/auth/sign-in", changeLoginHttpEntity, JwtDto.class);

        assertEquals(HttpStatus.UNAUTHORIZED, lockUserJwtDto.getStatusCode());
    }

    @DisplayName("일반 유저는 전체 유저 목록을 조회할 수 없다")
    @Test
    void createUserAndLoginWithGetAllUsersShouldFail() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        assertEquals("test@test.com",createdUser.getBody().email());
        assertEquals(HttpStatus.CREATED, createdUser.getStatusCode());

        SignInRequest signInRequest = new SignInRequest("test@test.com","password");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);

        assertEquals("test@test.com",loginJwtDto.getBody().userDto().email());
        assertNotNull(loginJwtDto.getBody().accessToken());
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpHeaders headers = getHttpHeaders(loginJwtDto);
        HttpEntity<Void> getAllUsersHttpEntity = new HttpEntity<>(headers);

        ResponseEntity<CursorResponseUserDto> findResponse = rest.exchange(
                "/api/users",
                HttpMethod.GET,
                getAllUsersHttpEntity,
                CursorResponseUserDto.class
        );
        assertEquals(HttpStatus.FORBIDDEN, findResponse.getStatusCode());
    }

    @DisplayName("어드민 계정은 전체 유저 목록 조회에 성공한다")
    @Test
    void loginAdminAndGetAllUsers() {
        UserCreateRequest request = new UserCreateRequest("test1","test1@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        UserCreateRequest request2 = new UserCreateRequest("test2","test2@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity2 = new HttpEntity<>(request2, defaultHeaders);

        ResponseEntity<UserDto> createdUser2 = rest.postForEntity("/api/users", httpEntity2, UserDto.class);


        SignInRequest signInRequest = new SignInRequest("admin@google.com","asdf1234!");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpHeaders headers = getHttpHeaders(loginJwtDto);
        HttpEntity<Void> getAllUsersHttpEntity = new HttpEntity<>(headers);
        String uri = "/api/users?limit=20&sortDirection=ASCENDING&sortBy=name";
        ResponseEntity<CursorResponseUserDto> findResponse = rest.exchange(
                uri,
                HttpMethod.GET,
                getAllUsersHttpEntity,
                CursorResponseUserDto.class
        );

        System.out.println(userRepository.count());
        assertEquals(HttpStatus.OK, findResponse.getStatusCode());
        assertEquals(3, findResponse.getBody().totalCount());
    }

    @DisplayName("어드민 계정으로 로그인 후 일반 유저의 권한을 수정한다")
    @Test
    void loginAdminAndUpdateRole() {
        UserCreateRequest request = new UserCreateRequest("test","test@test.com", "password");
        HttpEntity<UserCreateRequest> httpEntity = new HttpEntity<>(request, defaultHeaders);

        ResponseEntity<UserDto> createdUser = rest.postForEntity("/api/users", httpEntity, UserDto.class);

        SignInRequest signInRequest = new SignInRequest("admin@google.com","asdf1234!");
        HttpEntity loginHttpEntity = getSignInRequest(signInRequest);
        ResponseEntity<JwtDto> loginJwtDto = rest.postForEntity("/api/auth/sign-in", loginHttpEntity, JwtDto.class);
        assertEquals(HttpStatus.OK, loginJwtDto.getStatusCode());

        HttpHeaders headers = getHttpHeaders(loginJwtDto);
        UserRoleUpdateRequest roleUpdateRequest = new UserRoleUpdateRequest(Role.ADMIN);

        HttpEntity<UserRoleUpdateRequest> roleUpdateHttpEntity = new HttpEntity<>(roleUpdateRequest,headers);
        ResponseEntity<Void> roleUpdateResponse = rest.exchange(
                "/api/users/" + createdUser.getBody().id() + "/role",
                HttpMethod.PATCH,
                roleUpdateHttpEntity,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, roleUpdateResponse.getStatusCode());
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

    private HttpHeaders getHttpHeaders(ResponseEntity<JwtDto> loginJwtDto) {
        List<String> loginSetCookies = loginJwtDto.getHeaders().get(HttpHeaders.SET_COOKIE);
        String refreshCookie = loginSetCookies.stream()
                .filter(c -> c.startsWith("REFRESH_TOKEN"))
                .findFirst()
                .orElseThrow();

        String refreshCookieValue = refreshCookie.split(";", 2)[0];

        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.setContentType(MediaType.APPLICATION_JSON);
        refreshHeaders.add("Authorization", "Bearer " + loginJwtDto.getBody().accessToken());
        String xsrfCookie = defaultHeaders.getFirst(HttpHeaders.COOKIE);
        refreshHeaders.add(HttpHeaders.COOKIE, xsrfCookie + "; " + refreshCookieValue);
        refreshHeaders.add("X-XSRF-TOKEN", defaultHeaders.getFirst("X-XSRF-TOKEN"));
        return refreshHeaders;
    }
}
