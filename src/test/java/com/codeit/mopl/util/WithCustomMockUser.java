package com.codeit.mopl.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory= WithCustomMockUserSecurityContextFactory.class)
public @interface WithCustomMockUser {

    String email() default "test@test.com";
    String password() default "testPassword";
    String name() default "testName";
}