package com.codeit.mopl.domain.user.fixture;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class UserFixture {
    public static User createUser1() {
        return new User("test1@test.com", "password", "test1", Instant.now().minus(Duration.ofHours(4)));
    }

    public static User createUser2() {
        return new User("test2@test.com", "password", "test2", Instant.now().minus(Duration.ofHours(3)));
    }

    public static User createUser3() {
        return new User("test3@test.com", "password", "test3", Instant.now().minus(Duration.ofHours(2)));
    }

    public static User createAdmin() {
        User admin = new User("admin@admin.com", "admin!", "admin", Instant.now().minus(Duration.ofHours(1)));
        admin.setRole(Role.ADMIN);
        return admin;
    }

    public static UserDto createUserDto1() {
        return new UserDto(UUID.randomUUID(), Instant.now(), "test1@test.com", "test1", null, Role.USER, false);
    }

    public static UserDto createUserDto2() {
        return new UserDto(UUID.randomUUID(), Instant.now(), "test2@test.com", "test2",  null, Role.USER, false);
    }

    public static UserDto createUserDto3() {
        return new UserDto(UUID.randomUUID(), Instant.now(), "test3@test.com", "test3",  null, Role.USER, false);
    }

    public static UserDto createUserDto4() {
        return new UserDto(UUID.randomUUID(), Instant.now(), "admin@admin.com", "admin", null, Role.ADMIN, false);
    }
}
