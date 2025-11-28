package com.codeit.mopl.exception.message.directmessage;

import com.codeit.mopl.exception.message.MessageErrorCode;
import com.codeit.mopl.exception.message.MessageException;

import java.util.HashMap;
import java.util.UUID;

public class DirectMessageNotFound extends MessageException {
    private DirectMessageNotFound() {super(MessageErrorCode.DIRECT_MESSAGE_NOT_FOUND, new HashMap<>());}

    public static DirectMessageNotFound withId(UUID directMessageId) {
        DirectMessageNotFound ex = new DirectMessageNotFound();
        ex.getDetails().put("directMessageId", directMessageId);
        return ex;
    }
}
