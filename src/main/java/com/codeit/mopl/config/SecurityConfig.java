package com.codeit.mopl.config;

import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.security.CustomUserDetailsService;
import com.codeit.mopl.security.TempPasswordAuthenticationProvider;
import com.codeit.mopl.security.jwt.registry.JwtRegistry;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.filter.JwtAuthenticationFilter;
import com.codeit.mopl.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.codeit.mopl.security.jwt.handler.JwtLoginSuccessHandler;
import com.codeit.mopl.security.jwt.handler.JwtLogoutHandler;
import com.codeit.mopl.security.jwt.handler.LoginFailureHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

@Configuration
public class SecurityConfig {
    @Value("${admin.email}")
    private String adminEmail;
    @Value("${admin.name}")
    private String adminName;
    @Value("${admin.password}")
    private String adminPassword;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtTokenProvider jwtTokenProvider,
                                           UserDetailsService customUserDetailsService,
                                           JwtRegistry jwtRegistry,
                                           JwtLoginSuccessHandler jwtLoginSuccessHandler,
                                           LoginFailureHandler loginFailureHandler,
                                           JwtLogoutHandler jwtLogoutHandler,
                                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                                           TempPasswordAuthenticationProvider tempPasswordAuthenticationProvider,
                                           PasswordEncoder passwordEncoder) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/batch/**")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
//                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, jwtRegistry),
                        UsernamePasswordAuthenticationFilter.class)
                .formLogin(login ->
                    login.loginProcessingUrl("/api/auth/sign-in")
                            .successHandler(jwtLoginSuccessHandler)
                            .failureHandler(loginFailureHandler)
                )
                .logout(logout ->
                        logout.logoutUrl("/api/auth/sign-out")
                                .addLogoutHandler(jwtLogoutHandler)
                                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                                .accessDeniedHandler(new AccessDeniedHandlerImpl())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/batch/init").permitAll() // 초기 데이터 세팅
                        .requestMatchers("/api/auth/csrf-token").permitAll()  // csrf-token 조회
                        .requestMatchers("/api/auth/sign-in").permitAll()  // 로그인
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()  // 회원가입
                        .requestMatchers("/api/auth/refresh").permitAll()  // 토큰 재발급
                        .requestMatchers("/api/auth/reset-password").permitAll()  // 비밀번호 초기화
                        .requestMatchers("/ws/**").permitAll()  // 웹소켓
                        .requestMatchers( "*","/actuator/**", "/swagger-resource/**"
                                , "/swagger-ui.html", "/swagger-ui/**", "/v3/**",
                                "/assets/**","/h2/**").permitAll()
                        // ADMIN 권한이 있는 경우에만 접근 가능
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")  // 전체 회원 목록 조회
                        .requestMatchers(HttpMethod.POST, "/api/users/{userId}/role", "/api/users/{userId}/locked",
                                "/api/contents/").hasRole("ADMIN")  // 회원 권한 변경, 회원 잠금, 콘텐츠 생성
                        .requestMatchers(HttpMethod.DELETE, "/api/contents/{contentId}").hasRole("ADMIN")  // 콘텐츠 삭제
                        .requestMatchers(HttpMethod.PATCH, "/api/contents/{contentId}").hasRole("ADMIN")  // 콘텐츠 수정
                        .anyRequest().authenticated()
                );
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder.authenticationProvider(tempPasswordAuthenticationProvider)
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER");
        return roleHierarchy;
    }

    public class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();

        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            /*
             * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
             * the CsrfToken when it is rendered in the response body.
             */
            this.xor.handle(request, response, csrfToken);
            /*
             * Render the token value to a cookie by causing the deferred token to be loaded.
             */
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler
             * to resolve the CsrfToken. This applies when a single-page application includes
             * the header value automatically, which was obtained via a cookie containing the
             * raw CsrfToken.
             *
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
             * when a server-side rendered form includes the _csrf request parameter as a
             * hidden input.
             */
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
        }
    }

    @Bean
    public SessionRegistry sessionRegistry() {

        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }


    @Bean
    public UserDetailsService customUserDetailsService(UserRepository userRepository, UserMapper userMapper) {
        if(!userRepository.existsByEmail(adminEmail)) {
            String encodedPassword = passwordEncoder().encode(adminPassword);
            User admin = new User(adminEmail,encodedPassword,adminName);
            admin.updateRole(Role.ADMIN);
            userRepository.save(admin);
        }
        return new CustomUserDetailsService(userRepository, userMapper);
    }
}
