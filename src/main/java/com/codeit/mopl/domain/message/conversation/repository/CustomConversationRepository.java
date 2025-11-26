package com.codeit.mopl.domain.message.conversation.repository;

import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;

import java.util.List;

public interface CustomConversationRepository {
    List<Conversation> findAllByCond(ConversationSearchCond cond);

    long countAllByCond(ConversationSearchCond cond);
}
