package com.codeit.mopl.security.jwt.filter;

import com.codeit.mopl.exception.auth.AuthErrorCode;
import com.codeit.mopl.exception.auth.InvalidTokenException;
import com.codeit.mopl.security.jwt.registry.JwtRegistry;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final JwtRegistry jwtRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            if (!jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
                log.warn("[JWT 인증] Access Token이 유효하지 않음 token = {}", token);
                throw new InvalidTokenException(AuthErrorCode.TOKEN_INVALID, Map.of("type", "accessToken"));
            }
            jwtTokenProvider.verifyJws(token);
            String email = jwtTokenProvider.getEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            request.setAttribute("exception", e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String token = request.getHeader("Authorization");

        return token == null || !token.startsWith("Bearer ");
    }
}
