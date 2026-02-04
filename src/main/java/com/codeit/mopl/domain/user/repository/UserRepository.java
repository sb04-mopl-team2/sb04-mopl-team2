package com.codeit.mopl.domain.user.repository;

import com.codeit.mopl.domain.user.entity.Provider;
import com.codeit.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> , CustomUserRepository{

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByEmailAndProviderIsNot(String email, Provider provider);
}
