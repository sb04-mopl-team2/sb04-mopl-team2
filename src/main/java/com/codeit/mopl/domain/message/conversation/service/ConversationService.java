package com.codeit.mopl.domain.message.conversation.service;

import com.codeit.mopl.domain.message.conversation.dto.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.ConversationDto;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.mapper.ConversationMapper;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.message.conversation.ConversationDuplicateException;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;

    public ConversationDto createConversation(UUID loginUserId, ConversationCreateRequest request) {
        UUID conversationId = UUID.randomUUID();
        UUID withUserId = request.withUserId();
        log.info("[메세지] 채팅방 생성 시작 - conversationId = {}, receiverId = {} ", conversationId, withUserId);

        User loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> {
                    log.warn("[메세지] 채팅방 생성 실패 - 로그인 유저가 존재하지 않음 - userId = {}", loginUserId);
                    return new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", loginUserId));
                });

        User withUser = userRepository.findById(withUserId)
                .orElseThrow(() -> {
                    log.warn("[메세지] 채팅방 생성 실패 - 상대 유저가 존재하지 않음 - userId = {}", withUserId);
                    return new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", withUserId));
                });

        if (conversationRepository.existsByUserIdAndWithUserId(loginUserId, withUserId)) {
            log.info("[메세지] 채팅방 생성 실패 - 이미 생성된 채팅방임 - conversationId = {}", conversationId);
            throw ConversationDuplicateException.withId(conversationId);
        }

        Conversation conversation = Conversation.builder()
                .user(loginUser)
                .with(withUser)
                .hasUnread(false)
                .messages(new ArrayList<>())
                .build();

        Conversation saved = conversationRepository.save(conversation);
        log.info("[메세지] 채팅방 생성 완료 - conversationId = {}", saved.getId());
        return conversationMapper.toConversationDto(saved, null);
    }

}
