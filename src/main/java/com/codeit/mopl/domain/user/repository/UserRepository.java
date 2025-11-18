package com.codeit.mopl.domain.user.repository;

import com.codeit.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> , CustomUserRepository{

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying
    @Query("update User u set u.followerCount = u.followerCount + 1 where u.id = :userId")
    void increaseFollowerCount(UUID userId);

    long findFollowerCountById(UUID userId);
}
