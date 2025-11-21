package com.codeit.mopl.mail.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordUtils {
    public String makeTempPassword() {
        final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
        final String NUMBERS = "0123456789";
        final String ALL_CHARS = UPPERCASE + LOWERCASE + NUMBERS;

        final int MIN_LENGTH = 8;
        final int MAX_LENGTH = 16;

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        int passwordLength = random.nextInt(MIN_LENGTH, MAX_LENGTH);
        for (int i = 0; i < passwordLength; i++) {
            sb.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }

        return sb.toString();
    }

}
