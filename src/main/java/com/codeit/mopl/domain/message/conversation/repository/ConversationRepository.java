package com.codeit.mopl.domain.message.conversation.repository;

import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID>, CustomConversationRepository {
    //boolean existsByUser_IdAndWith_Id(UUID userA, UUID userB);

    Optional<Conversation> findByUser_IdAndWith_Id(UUID userA, UUID userB);

}
