package com.codeit.mopl.domain.user.repository;

import com.codeit.mopl.config.QuerydslConfig;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.codeit.mopl.domain.user.dto.request.CursorRequestUserDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.fixture.UserFixture;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DataJpaTest
@Import(QuerydslConfig.class)
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private ContentMapper contentMapper;

    @AfterEach
    public void tearDown() {
        userRepository.deleteAll();
    }

    @BeforeEach
    public void setup() {
        userRepository.save(UserFixture.createUser1());  // test1@test.com, test1 4시간 전 생성
        userRepository.save(UserFixture.createUser2());  // test2@test.com, test2 3시간 전 생성
        userRepository.save(UserFixture.createUser3());  // test3@test.com, test3 2시간 전 생성
        userRepository.save(UserFixture.createAdmin());  // admin@admin.com, admin 1시간 전 생성
    }

    @DisplayName("EmailLike가 test일 때 총 3건이 조회된다.")
    @Test
    void countTotalElementsShouldReturn3WhenEmailLikeIsTest() {
        // when
        Long count = userRepository.countTotalElements("test");

        // then
        // test1@test.com, test2@test.com, test3@test.com
        assertEquals(3L,count);
    }

    @DisplayName("EmailLike가 admin일 때 총 1건이 조회된다.")
    @Test
    void countTotalElementsShouldReturn1WhenEmailLikeIsAdmin() {
        // when
        Long count = userRepository.countTotalElements("admin");

        // then
        // admin@admin.com
        assertEquals(1, count);
    }

    @DisplayName("EmailLike가 주어지지 않으면 전체(4건)이 조회된다.")
    @Test
    void countTotalElementsShouldReturn4WhenEmailLikeIsNull() {
        // when
        Long count = userRepository.countTotalElements(null);

        // then
        assertEquals(4L,count);
    }

    @DisplayName("emailLike test, sortBy name, 정방향 정렬 locked false, 그외 null -> 3건이 조회된다.")
    @Test
    void findAllPage1() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                "test",
                null,
                false,
                null,
                null,
                20,
                "ASCENDING",
                "name"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(3,response.getContent().size());
    }

    @DisplayName("sortBy name, 정방향 정렬, locked false 그외 null -> 4건이 조회된다.")
    @Test
    void findAllPage_EmailLike_Null() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                null,
                null,
                20,
                "ASCENDING",
                "name"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(4,response.getContent().size());
    }

    @DisplayName("sortBy name, 정방향 정렬, cursor test1 그외 null -> test2, test3 2건이 조회된다.")
    @Test
    void findAllPage_UseCursor() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "test1",
                null,
                20,
                "ASCENDING",
                "name"
        );

        // when
        Slice<UserDto>response = userRepository.findAllPage(request);

        // then
        assertEquals(2,response.getContent().size());
    }

    @DisplayName("sortBy name, 역방향 정렬, cursor test1, locked false 그외 null -> admin 1건이 조회된다.")
    @Test
    void findAllPage_UseCursor_Desc() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "test1",
                null,
                20,
                "DESCENDING",
                "name"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(1,response.getContent().size());
    }

    @DisplayName("검색 결과가 해당하는 유저가 없으면 빈 데이터를 반환한다")
    @Test
    void findAllPage_NoContent() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                "spring",
                Role.USER,
                false,
                null,
                null,
                20,
                "ASCENDING",
                "name"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(0,response.getContent().size());
    }

    @DisplayName("limit 제한이 있으면 limit의 사이즈만큼만 결과를 반환하고 hasNext필드가 true로 반환된다.")
    @Test
    void findAllPage_hasNextTrue() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                null,
                null,
                1,
                "ASCENDING",
                "name"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(1,response.getContent().size());
        assertEquals(true, response.hasNext());
    }

    @DisplayName("RoleEquals가 주어지면 해당 Role을 가진 유저만 조회된다. -> admin 1건 조회")
    @Test
    void findAllPage_roleEqual_ADMIN() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                Role.ADMIN,
                false,
                 null,
                null,
                20,
                "ASCENDING",
                "name"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(1, response.getContent().size());
        assertEquals(Role.ADMIN, response.getContent().get(0).role());
    }

    @DisplayName("sortBy email, 정방향 정렬, cursor test1@test.com 그외 null -> test2, test3 2건이 조회된다.")
    @Test
    void findAllPageUseCursorSortByEmailASC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "test1@test.com",
                null,
                20,
                "ASCENDING",
                "email"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(2,response.getContent().size());
    }

    @DisplayName("sortBy email, 역방향 정렬, cursor test1@test.com, locked false 그외 null -> admin 1건이 조회된다.")
    @Test
    void findAllPageUseCursorSortByEmailDESC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "test1@test.com",
                null,
                20,
                "DESCENDING",
                "email"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(1,response.getContent().size());
    }

    @DisplayName("sortBy createdAt, 정방향 정렬, cursor 현재 그 외 null -> 0건 조회")
    @Test
    void findAllPageUseCursorSortByCreatedAtASC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                Instant.now().toString(),
                null,
                20,
                "ASCENDING",
                "createdAt"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(0,response.getContent().size());
    }

    @DisplayName("sortBy createdAt, 역방향 정렬, cursor 현재 그 외 null -> 전체(4건) 조회")
    @Test
    void findAllPageUseCursorSortByCreatedAtDESC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                Instant.now().toString(),
                null,
                20,
                "DESCENDING",
                "createdAt"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(4,response.getContent().size());
        assertTrue(response.getContent().get(0).createdAt().isBefore(Instant.now()));
        assertTrue(response.getContent().get(0).createdAt()
                .isAfter(response.getContent().get(1).createdAt()));
    }

    @DisplayName("sortBy isLocked, 정방향 정렬, cursor false 그외 null -> 최초 1건 isLocked = false")
    @Test
    void findAllPageUseCursorSortByIsLockedFalseASC() {
        // given
        // lock 테스트용 lockUser
        User lockedUser = new User("locked@lock.com", "password", "locked");
        lockedUser.updateLock(true);
        userRepository.save(lockedUser);

        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "false",
                null,
                20,
                "ASCENDING",
                "isLocked"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(false, response.getContent().get(0).locked());

    }

    @DisplayName("sortBy isLocked, 정방향 정렬, cursor true 그외 null -> admin 1건이 조회된다.")
    @Test
    void findAllPageUseCursorSortByIsLockedTrueASC() {
        // given
        // lock 테스트용 lockUser
        User lockedUser = new User("locked@lock.com", "password", "locked");
        lockedUser.updateLock(true);
        userRepository.save(lockedUser);

        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                "true",
                null,
                20,
                "ASCENDING",
                "isLocked"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(true, response.getContent().get(0).locked());
    }

    @DisplayName("sortBy isLocked, 역방향 정렬, cursor false 그외 null -> 최초 1건 isLocked = false")
    @Test
    void findAllPageUseCursorSortByIsLockedFalseDESC() {
        // given
        // lock 테스트용 lockUser
        User lockedUser = new User("locked@lock.com", "password", "locked");
        lockedUser.updateLock(true);
        userRepository.save(lockedUser);

        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "false",
                null,
                20,
                "DESCENDING",
                "isLocked"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(false, response.getContent().get(0).locked());

    }

    @DisplayName("sortBy isLocked, 역방향 정렬, cursor true 그외 null -> admin 1건이 조회된다.")
    @Test
    void findAllPageUseCursorSortByIsLockedTrueDESC() {
        // given
        // lock 테스트용 lockUser
        User lockedUser = new User("locked@lock.com", "password", "locked");
        lockedUser.updateLock(true);
        userRepository.save(lockedUser);

        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                "true",
                null,
                20,
                "DESCENDING",
                "isLocked"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(true, response.getContent().get(0).locked());
    }

    @DisplayName("sortBy isLocked, 정방향 정렬 시 첫번째는 false")
    @Test
    void findAllPageUseCursorSortByIsLockedASC() {
        // given
        // lock 테스트용 lockUser
        User lockedUser = new User("locked@lock.com", "password", "locked");
        lockedUser.updateLock(true);
        userRepository.save(lockedUser);

        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                null,
                null,
                20,
                "ASCENDING",
                "isLocked"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(false, response.getContent().get(0).locked());
    }

    @DisplayName("sortBy isLocked, 역방향 정렬 시 첫번째는 true")
    @Test
    void findAllPageUseCursorSortByIsLockedDESC() {
        // given
        // lock 테스트용 lockUser
        User lockedUser = new User("locked@lock.com", "password", "locked");
        lockedUser.updateLock(true);
        userRepository.save(lockedUser);

        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                null,
                null,
                20,
                "DESCENDING",
                "isLocked"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(true, response.getContent().get(0).locked());
    }

    @DisplayName("sortBy role, 정방향 정렬, cursor user 그외 null -> 최초 1건 role user")
    @Test
    void findAllPageUseCursorSortByRoleUserASC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "USER",
                null,
                20,
                "ASCENDING",
                "role"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(Role.USER, response.getContent().get(0).role());
    }

    @DisplayName("sortBy role, 정방향 정렬, cursor admin 그외 null -> admin 1건이 조회된다.")
    @Test
    void findAllPageUseCursorSortByRoleAdminASC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                "ADMIN",
                null,
                20,
                "ASCENDING",
                "role"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(Role.ADMIN, response.getContent().get(0).role());
    }

    @DisplayName("sortBy role, 역방향 정렬, cursor user 그외 null -> 최초 1건 role user")
    @Test
    void findAllPageUseCursorSortByRoleUserDESC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                false,
                "USER",
                null,
                20,
                "DESCENDING",
                "role"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(Role.USER, response.getContent().get(0).role());
    }

    @DisplayName("sortBy role, 역방향 정렬, cursor admin 그외 null -> 최초는 ADMIN")
    @Test
    void findAllPageUseCursorSortByRoleAdminDESC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                "ADMIN",
                null,
                20,
                "DESCENDING",
                "role"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(Role.ADMIN, response.getContent().get(0).role());
    }

    @DisplayName("sortBy role, 역방향 정렬시 최초 유저는 user이다")
    @Test
    void findAllPageUseCursorSortByRoleDESC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                null,
                null,
                20,
                "DESCENDING",
                "role"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(Role.USER, response.getContent().get(0).role());
    }

    @DisplayName("sortBy role, 정방향 정렬시 최초 유저는 admin이다")
    @Test
    void findAllPageUseCursorSortByRoleASC() {
        // given
        CursorRequestUserDto request = new CursorRequestUserDto(
                null,
                null,
                null,
                null,
                null,
                20,
                "ASCENDING",
                "role"
        );

        // when
        Slice<UserDto> response = userRepository.findAllPage(request);

        // then
        assertEquals(Role.ADMIN, response.getContent().get(0).role());
    }
}
