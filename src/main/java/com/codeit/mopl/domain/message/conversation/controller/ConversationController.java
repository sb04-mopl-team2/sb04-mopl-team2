package com.codeit.mopl.domain.message.conversation.controller;

import com.codeit.mopl.domain.message.conversation.service.ConversationService;
import com.codeit.mopl.domain.message.directmessage.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;
    private final DirectMessageService directMessageService;

}
