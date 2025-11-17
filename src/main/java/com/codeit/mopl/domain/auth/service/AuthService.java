package com.codeit.mopl.domain.auth.service;

import com.codeit.mopl.domain.auth.dto.request.ResetPasswordRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import com.codeit.mopl.exception.user.ErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.mail.service.MailService;
import com.codeit.mopl.mail.utils.PasswordUtils;
import com.codeit.mopl.security.jwt.JwtTokenProvider;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordUtils passwordUtils;
    private final MailService mailService;

    @Transactional
    public String reissueAccessToken(Map<String, Object> claims, UserDto userDto) {
        log.info("AccessToken 재발급 시도");
        Date expiredAt = (Date) claims.get("exp");
        if (expiredAt.before(new Date())) {
            log.warn("[JWT] RefreshToken 유효기간이 만료 됨");
            throw new InvalidTokenException(com.codeit.mopl.exception.auth.ErrorCode.TOKEN_INVALID,Map.of("type","refresh","expiredAt",expiredAt));
        }

        return jwtTokenProvider.generateAccessToken(claims, userDto.email());
    }

    @Transactional
    public String reissueRefreshToken(Map<String, Object> claims, UserDto userDto) {
        log.info("RefreshToken 재발급 시도");
        Date expiredAt = (Date) claims.get("exp");
        if (expiredAt.before(new Date())) {
            log.warn("[JWt] RefreshToken 유효기간이 만료 됨");
            throw new InvalidTokenException(com.codeit.mopl.exception.auth.ErrorCode.TOKEN_INVALID,Map.of("type","refresh","expiredAt",expiredAt));
        }

        return jwtTokenProvider.generateRefreshToken(claims, userDto.email());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) throws MessagingException {
        log.info("[사용자 관리] 패스워드 초기화 요청 email = {}", request.email());
        User findUser = findUserByEmail(request.email());
        String tempPw = passwordUtils.makeTempPassword();
        String encodedPw = passwordEncoder.encode(tempPw);
        findUser.setPassword(encodedPw);
        log.debug("[사용자 관리] 패스워드 초기화 password = {}", tempPw);
        userRepository.save(findUser);
        log.info("[사용자 관리] 패스워드 초기화 완료 userId = {}", findUser.getId());
        log.info("[이메일] 이메일 전송 email = {}", request.email());
        mailService.sendMail(request.email(), tempPw);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND,Map.of("email", email)));
    }
}
