package com.codeit.mopl.exception.message.conversation;

import com.codeit.mopl.exception.message.MessageErrorCode;
import com.codeit.mopl.exception.message.MessageException;

import java.util.HashMap;
import java.util.UUID;

public class ConversationForbiddenException extends MessageException {
    private ConversationForbiddenException() { super(MessageErrorCode.CONVERSATION_ACCESS_FORBIDDEN, new HashMap<>());}

    public static ConversationForbiddenException withId(UUID loginUserId) {
        ConversationForbiddenException ex = new ConversationForbiddenException();
        ex.getDetails().put("loginUserId", loginUserId);
        return ex;
    }
}
