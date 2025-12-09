package com.codeit.mopl.domain.auth.service;

import com.codeit.mopl.domain.auth.dto.request.ResetPasswordRequest;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Provider;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.MailSendEvent;
import com.codeit.mopl.exception.auth.AuthErrorCode;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import com.codeit.mopl.exception.user.SocialAccountPasswordChangeNotAllowedException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.mail.utils.PasswordUtils;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordUtils passwordUtils;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public String reissueAccessToken(Map<String, Object> claims, UserDto userDto) {
        log.info("AccessToken 재발급 시도");
        Date expiredAt = (Date) claims.get("exp");
        if (expiredAt.before(new Date())) {
            log.warn("[JWT] RefreshToken 유효기간이 만료 됨");
            throw new InvalidTokenException(AuthErrorCode.TOKEN_INVALID,Map.of("type","access","expiredAt",expiredAt));
        }

        return jwtTokenProvider.generateAccessToken(claims, userDto.email());
    }

    @Transactional
    public String reissueRefreshToken(Map<String, Object> claims, UserDto userDto) {
        log.info("RefreshToken 재발급 시도");
        Date expiredAt = (Date) claims.get("exp");
        if (expiredAt.before(new Date())) {
            log.warn("[JWT] RefreshToken 유효기간이 만료 됨");
            throw new InvalidTokenException(AuthErrorCode.TOKEN_INVALID,Map.of("type","refresh","expiredAt",expiredAt));
        }

        return jwtTokenProvider.generateRefreshToken(claims, userDto.email());
    }

    @Transactional(rollbackFor = MessagingException.class)
    public void resetPassword(ResetPasswordRequest request) throws MessagingException {
        log.info("[사용자 관리] 패스워드 초기화 요청 email = {}", request.email());
        if (!userRepository.existsByEmail(request.email())) {
            log.debug("[사용자 관리] 해당 유저를 찾을 수 없음 email = {}", request.email());
            throw new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("email", request.email()));
        }
        checkProviderIsLocal(request.email());

        String tempPw = passwordUtils.makeTempPassword();

        publisher.publishEvent(new MailSendEvent(UUID.randomUUID(), request.email(), tempPw));
    }

    private void checkProviderIsLocal(String email) {
        if (userRepository.existsByEmailAndProviderIsNot(email, Provider.LOCAL)) {
            throw new SocialAccountPasswordChangeNotAllowedException(UserErrorCode.SOCIAL_ACCOUNT_CHANGE_PASSWORD_NOT_ALLOWED, Map.of("email",email));
        }
    }
}
