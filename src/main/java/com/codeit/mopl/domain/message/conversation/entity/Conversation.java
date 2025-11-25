package com.codeit.mopl.domain.message.conversation.entity;

import com.codeit.mopl.domain.base.UpdatableEntity;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "conversations",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "with_user_id"},
                name = "uk_conversation_users"))
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Conversation extends UpdatableEntity {

    // 대화방을 소유하고 있는 사용자 (본인)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 대화의 상대방
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "with_user_id", nullable = false)
    private User with;

    // 메세지를 읽었는지 여부
    @Column(nullable = false)
    private boolean hasUnread;

    @Builder.Default
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DirectMessage> messages = new ArrayList<>();
}
