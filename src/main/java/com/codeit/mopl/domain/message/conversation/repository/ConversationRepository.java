package com.codeit.mopl.domain.message.conversation.repository;

import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

}
