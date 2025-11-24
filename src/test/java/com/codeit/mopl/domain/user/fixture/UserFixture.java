package com.codeit.mopl.domain.user.fixture;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserFixture {
    public static User createUser1() {
        return new User("test1@test.com", "password", "test1", LocalDateTime.now().minusHours(4));
    }

    public static User createUser2() {
        return new User("test2@test.com", "password", "test2", LocalDateTime.now().minusHours(3));
    }

    public static User createUser3() {
        return new User("test3@test.com", "password", "test3", LocalDateTime.now().minusHours(2));
    }

    public static User createUser4() {
        User admin = new User("admin@admin.com", "password", "admin", LocalDateTime.now().minusHours(1));
        admin.setRole(Role.ADMIN);
        return admin;
    }

    public static UserDto createUserDto1() {
        return new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test1@test.com", "test1", null, Role.USER, false);
    }

    public static UserDto createUserDto2() {
        return new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test2@test.com", "test2",  null, Role.USER, false);
    }

    public static UserDto createUserDto3() {
        return new UserDto(UUID.randomUUID(), LocalDateTime.now(), "test3@test.com", "test3",  null, Role.USER, false);
    }

    public static UserDto createUserDto4() {
        return new UserDto(UUID.randomUUID(), LocalDateTime.now(), "admin@test.com", "admin", null, Role.ADMIN, false);
    }
}
