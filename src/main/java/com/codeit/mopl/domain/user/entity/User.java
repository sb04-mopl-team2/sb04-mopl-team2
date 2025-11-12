package com.codeit.mopl.domain.user.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(nullable = false)
    private long followerCount;

    public User(String email, String password, String name, String profileImageUrl) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.role = Role.USER;
        this.locked = false;
        this.followerCount = 0;
    }

    public void updateRole(Role role) {
        this.role = role;
    }
}
