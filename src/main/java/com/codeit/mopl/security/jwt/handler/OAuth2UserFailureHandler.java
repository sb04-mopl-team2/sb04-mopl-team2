package com.codeit.mopl.security.jwt.handler;

import com.codeit.mopl.exception.auth.OAuth2LockedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2UserFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest req, HttpServletResponse res,
                                        AuthenticationException ex) throws IOException {
        if (ex instanceof OAuth2LockedException) {
            getRedirectStrategy().sendRedirect(req, res, "/");
        }
    }
}
