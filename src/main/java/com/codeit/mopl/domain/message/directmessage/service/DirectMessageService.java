package com.codeit.mopl.domain.message.directmessage.service;


import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.directmessage.dto.CursorResponseDirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSearchCond;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSendRequest;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.message.directmessage.mapper.DirectMessageMapper;
import com.codeit.mopl.domain.message.directmessage.repository.DirectMessageRepository;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.exception.message.conversation.ConversationNotFound;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DirectMessageService {
    private final DirectMessageRepository directMessageRepository;
    private final DirectMessageMapper directMessageMapper;
    private final ConversationRepository conversationRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CursorResponseDirectMessageDto getDirectMessages(UUID loginUserId,
                                                            UUID conversationId,
                                                            DirectMessageSearchCond cond) {
        log.info("[메세지] 해당 채팅방의 DM 목록 조회 시작 - loginUserId = {} conversation {}", loginUserId, conversationId);
        List<DirectMessage> directMessages;
        if (cond.getSortDirection() ==SortDirection.DESCENDING) {
            directMessages = directMessageRepository.findMessagesBefore(
                    conversationId,
                    cond.getCursor(),
                    cond.getIdAfter()
            );
        } else {
            directMessages = directMessageRepository.findMessagesAfter(
                    conversationId,
                    cond.getCursor(),
                    cond.getIdAfter()
            );
        }
        //빈 리스트 체크
        if (directMessages.isEmpty()) {
            return new CursorResponseDirectMessageDto(
                    new ArrayList<>(),
                    null,
                    null,
                    false,
                    0L,
                    cond.getSortBy(),
                    cond.getSortDirection()
            );
        }
        int originalSize = directMessages.size();
        boolean hasNext = originalSize > cond.getLimit();
        List<DirectMessage> result = hasNext
                ? directMessages.subList(0, cond.getLimit())
                : directMessages;
        DirectMessage last = result.get(result.size() - 1);
        String nextCursor = hasNext ? last.getCreatedAt().toString() : null;
        UUID nextAfter = hasNext ? last.getId() : null;

        List<DirectMessageDto> directMessageDtos =
                result.stream()
                        .map(directMessageMapper::toDirectMessageDto)
                        .collect(Collectors.toList());
        long totalCount = directMessageRepository.countAllByConversationId(conversationId);
        log.info("[메세지] 해당 채팅방의 DM 목록 조회 완료 - conversationId = {}, totalCount = {}", conversationId, totalCount);
        return new CursorResponseDirectMessageDto(
                directMessageDtos,
                nextCursor,
                nextAfter,
                hasNext,
                totalCount,
                cond.getSortBy(),
                cond.getSortDirection()
        );
    }

    public DirectMessageDto saveDirectMessage(UUID loginUserId,
                                              DirectMessageSendRequest request
                                              ){
        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> ConversationNotFound.withId(request.receiverId()));
        User sender = userRepository.findById(loginUserId)
                .orElseThrow(()-> new  UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", loginUserId)));
        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(()-> new  UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", request.receiverId())));
        DirectMessage directMessage = directMessageRepository.save(DirectMessage.builder()
                .sender(sender)
                .receiver(receiver)
                .conversation(conversation)
                .content(request.content())
                .isRead(false)
                .build()
        );
        return directMessageMapper.toDirectMessageDto(directMessage);
    }
}
