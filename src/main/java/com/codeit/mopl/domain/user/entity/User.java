package com.codeit.mopl.domain.user.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends UpdatableEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String profileImageUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column
    private boolean locked;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private long followerCount;

    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileImageUrl = null;
        this.role = Role.USER;
        this.locked = false;
        this.followerCount = 0;
    }

    public User(String email, String password, String name, String profileImageUrl) {
        this(email,password,name);
        this.profileImageUrl = profileImageUrl;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateLock(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User user)) return false;
        return getId() == user.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    public User(String email, String password, String name, LocalDateTime createdAt) {
        this(email,password,name);
        this.createdAt = createdAt;
    }

    public void increaseFollowerCount() {
        this.followerCount++;
    }

    public void decreaseFollowerCount() {
        this.followerCount--;
    }
}
