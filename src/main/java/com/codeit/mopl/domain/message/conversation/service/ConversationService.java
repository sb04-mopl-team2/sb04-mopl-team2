package com.codeit.mopl.domain.message.conversation.service;

import com.codeit.mopl.domain.message.conversation.dto.request.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.dto.response.ConversationDto;
import com.codeit.mopl.domain.message.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.mapper.ConversationMapper;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.message.directmessage.mapper.DirectMessageMapper;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.message.conversation.ConversationDuplicateException;
import com.codeit.mopl.exception.message.conversation.ConversationForbiddenException;
import com.codeit.mopl.exception.message.conversation.ConversationNotFound;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;
    private final DirectMessageMapper directMessageMapper;

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

    @Transactional(readOnly = true)
    public CursorResponseConversationDto getAllConversations(UUID loginUserId ,ConversationSearchCond cond) {
        log.info("[메세지] 채팅방 목록 조회 시작 - loginUserId = {}", loginUserId);
        List<Conversation> conversations = conversationRepository.findAllByCond(cond);

        // 빈 리스트에 대한 체크
        if (conversations.isEmpty()) {
            log.info("[메세지] 채팅방 목록 조회 완료 - 결과 없음");
            return new CursorResponseConversationDto(
                    new ArrayList<>(),
                    null,
                    null,
                    false,
                    0L,
                    cond.getSortBy(),
                    cond.getSortDirection()
            );
        }
        int originalSize = conversations.size();
        boolean hasNext = originalSize > cond.getLimit();

        List<Conversation> result = hasNext ? conversations.subList(0, cond.getLimit()) : conversations;
        Conversation lastConversation = result.get(result.size() - 1);
        String nextCursor = hasNext ? lastConversation.getCreatedAt().toString() : null;
        UUID nextIdAfter = hasNext ? lastConversation.getId() : null;

        List<ConversationDto> conversationDtos =
                result.stream()
                        .map(conversation -> {
                            List<DirectMessage> msgs = conversation.getMessages();
                            DirectMessage lastMessage = (msgs == null || msgs.isEmpty())
                                    ? null
                                    : msgs.get(msgs.size() - 1);

                            DirectMessageDto lastMessageDto =
                                    lastMessage == null ? null : directMessageMapper.toDirectMessageDto(lastMessage);
                            return conversationMapper.toConversationDto(conversation, lastMessageDto);
                        })
                        .collect(Collectors.toList());
        long totalCount = conversationRepository.countAllByCond(cond);
        log.info("[메세지] 채팅방 목록 조회 완료 - conversationId = {}", nextIdAfter);
        return new CursorResponseConversationDto(
                conversationDtos,
                nextCursor,
                nextIdAfter,
                hasNext,
                totalCount,
                cond.getSortBy(),
                cond.getSortDirection()
        );
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversationById(UUID loginUserId ,UUID conversationId) {
        log.info("[메세지] 채팅방 정보 조회 시작 - conversationId = {}", conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(()->{
                    log.warn("[메세지] 채팅방 정보 조회 실패 - 채팅방 존재하지 않음 - conversationId = {}", conversationId);
                    return ConversationNotFound.withId(conversationId);
                });

        //유저 권한 검증
        UUID userIdA = conversation.getUser().getId();
        UUID userB = conversation.getWith().getId();

        if (!userIdA.equals(loginUserId) && !userB.equals(loginUserId)) {
            log.warn("[메세지] 채팅방 정보 조회 실패 - 접근 권한 없음 - (loginUserId = {}, conversationId = {})", loginUserId, conversationId );
            throw ConversationForbiddenException.withId(conversationId);
        }

        List<DirectMessage> messages = conversation.getMessages();
        DirectMessage lastMessage =
                (messages == null || messages.isEmpty()) ? null
                        : messages.get(messages.size() - 1);
        DirectMessageDto lastMessageDto =
                lastMessage == null ? null : directMessageMapper.toDirectMessageDto(lastMessage);
        log.info("[메세지] 채팅방 정보 조회 완료 - conversationId = {}", conversationId);
        return conversationMapper.toConversationDto(conversation, lastMessageDto);
    }
}
