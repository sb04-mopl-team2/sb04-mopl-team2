package com.codeit.mopl.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ProfileChecker {

    private final Environment environment;

    public boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
