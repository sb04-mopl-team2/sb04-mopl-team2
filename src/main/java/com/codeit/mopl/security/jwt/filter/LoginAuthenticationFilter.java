package com.codeit.mopl.security.jwt.filter;

import com.codeit.mopl.security.token.TempPasswordAuthenticationToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
public class LoginAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final StringRedisTemplate redisTemplate;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String username = obtainUsername(request);
        String password = obtainPassword(request);

        String encodedTempPw = null;
        try {
            encodedTempPw = redisTemplate.opsForValue().get(username);
        } catch (Exception ignored) {
        }

        Authentication authRequest = (encodedTempPw != null)
                ? new TempPasswordAuthenticationToken(username, password, encodedTempPw)
                : new UsernamePasswordAuthenticationToken(username, password);

        setDetails(request, (UsernamePasswordAuthenticationToken) authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
