package com.codeit.mopl.domain.follow.entity;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@Table(name = "follows",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_follower_followee",
                        columnNames = {"follower_id", "followee_id"}
                )
        })
@NoArgsConstructor
public class Follow extends BaseEntity {
    public static final int MAX_RETRY_COUNT = 3;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User followee;

    @Enumerated(EnumType.STRING)
    @Column(name = "followStatus", nullable = false)
    private FollowStatus followStatus = FollowStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    public Follow(User follower, User followee) {
        this.follower = follower;
        this.followee = followee;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }
}
