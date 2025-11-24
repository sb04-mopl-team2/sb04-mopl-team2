package com.codeit.mopl.domain.message.conversation.repository;

import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    boolean existsByUser_IdAndWithUser_Id(UUID userA, UUID userB);
}
