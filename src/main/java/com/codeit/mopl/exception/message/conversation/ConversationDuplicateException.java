package com.codeit.mopl.exception.message.conversation;

import com.codeit.mopl.exception.message.MessageErrorCode;
import com.codeit.mopl.exception.message.MessageException;

import java.util.HashMap;
import java.util.UUID;

public class ConversationDuplicateException extends MessageException {
    private ConversationDuplicateException() {
        super(MessageErrorCode.CONVERSATION_ALREADY_EXIST, new HashMap<>());
    }

    public static ConversationDuplicateException withId(UUID conversationId) {
        ConversationDuplicateException ex = new ConversationDuplicateException();
        ex.getDetails().put("conversationId", conversationId);
        return ex;
    }
}
