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
        UUID withUserId = request.withUserId();
        if (loginUserId.equals(withUserId)) {
            log.warn("[메세지] 채팅방 생성 실패 - 본인과의 대화는 생성할 수 없음 - userId = {}", loginUserId);
            throw new IllegalArgumentException("본인과의 대화는 생성할 수 없습니다.");
        }

        log.info("[메세지] 채팅방 생성 시작 - loginUser = {}, receiverId = {} ", loginUserId, withUserId);

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

        // loginUserId, withUserId 중 더 작은 쪽을 userA, 더 큰 쪽을 userB로 설정하여 순서를 고정시킴
        UUID userA = loginUserId.compareTo(withUserId) < 0 ? loginUserId : withUserId;
        UUID userB = loginUserId.compareTo(withUserId) < 0 ? withUserId : loginUserId;

        Optional<Conversation> existing =
                conversationRepository.findByUser_IdAndWith_Id(userA, userB);

        if (existing.isPresent()) {
            UUID existingConversationId = existing.get().getId();
            log.info("[메세지] 채팅방 생성 실패 - 이미 생성된 채팅방임 - 기존 conversationId = {}", existingConversationId);
            throw ConversationDuplicateException.withId(existingConversationId);
        }

        Conversation conversation = Conversation.builder()
                .user(userA.equals(loginUserId) ? loginUser : withUser)
                .with(userB.equals(loginUserId) ? loginUser : withUser)
                .hasUnread(false)
                .messages(new ArrayList<>())
                .build();

        Conversation saved = conversationRepository.save(conversation);
        log.info("[메세지] 채팅방 생성 완료 - conversationId = {}", saved.getId());
        return conversationMapper.toConversationDto(saved, null);
    }
}
