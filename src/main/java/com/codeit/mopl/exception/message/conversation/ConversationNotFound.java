package com.codeit.mopl.exception.message.conversation;

import com.codeit.mopl.exception.message.MessageErrorCode;
import com.codeit.mopl.exception.message.MessageException;

import java.util.HashMap;
import java.util.UUID;

public class ConversationNotFound extends MessageException {
    private ConversationNotFound() { super(MessageErrorCode.CONVERSATION_NOT_FOUND, new HashMap<>());}

    public static ConversationNotFound withId(UUID withUserId) {
        ConversationNotFound ex = new ConversationNotFound();
        ex.getDetails().put("withUserId", withUserId);
        return ex;
    }

    public static ConversationNotFound of() {
        return new ConversationNotFound();
    }
}
