package com.codeit.mopl.domain.message.service;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.entity.SortBy;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.directmessage.dto.CursorResponseDirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSearchCond;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSendRequest;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.message.directmessage.mapper.DirectMessageMapper;
import com.codeit.mopl.domain.message.directmessage.repository.DirectMessageRepository;
import com.codeit.mopl.domain.message.directmessage.service.DirectMessageService;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.user.dto.response.UserSummary;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.user.UserNotFoundException;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class DirectMessageServiceTest {
    @Mock private DirectMessageRepository directMessageRepository;
    @Mock private DirectMessageMapper directMessageMapper;
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private DirectMessageService directMessageService;

    @Nested
    @DisplayName("createDM()")
    class createDirectMessage {

        @Test
        @DisplayName("정상 요청 시 DM을 저장함")
        void shouldSaveDirectMessage() {
            UUID loginUserId = UUID.randomUUID();
            User localUser = new User();
            setId(localUser, loginUserId);
            UserSummary sender = new UserSummary(loginUserId,"test1","test1");

            UUID receiverUserId = UUID.randomUUID();
            User receiverUser = new User();
            setId(receiverUser, receiverUserId);
            UserSummary receiver = new UserSummary(receiverUserId,"test2","test2");

            UUID conversationId = UUID.randomUUID();
            DirectMessageSendRequest request = new DirectMessageSendRequest(
                    "test"
            );
            Conversation conversation = Conversation.builder()
                    .user(localUser)
                    .with(receiverUser)
                    .hasUnread(false)
                    .messages(null)
                    .build();
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
            given(userRepository.findById(loginUserId)).willReturn(Optional.of(localUser));
            given(userRepository.findById(receiverUserId)).willReturn(Optional.of(receiverUser));

            DirectMessage message = DirectMessage.builder()
                    .sender(localUser)
                    .receiver(receiverUser)
                    .conversation(conversation)
                    .content("test")
                    .isRead(false)
                    .build();
            given(directMessageRepository.save(any(DirectMessage.class)))
                    .willReturn(message);            given(directMessageMapper.toDirectMessageDto(message))
                    .willReturn(new DirectMessageDto(
                       message.getId(),
                       conversationId,
                       Instant.now(),
                       sender,
                       receiver,
                       message.getContent()
                    ));
            //when
            DirectMessageDto result = directMessageService.saveDirectMessage(loginUserId,conversationId, request);

            //then
            verify(conversationRepository).findById(conversationId);
            verify(userRepository).findById(loginUserId);
            verify(userRepository).findById(receiverUserId);
            verify(directMessageMapper).toDirectMessageDto(message);
            verify(directMessageRepository).save(any(DirectMessage.class));
        }

        @Test
        @DisplayName("상대 유저가 존재하지 않는 경우 예외 발생")
        void shouldThrowExceptionWhenUserNotFound() {
            UUID loginUserId = UUID.randomUUID();
            User localUser = new User();
            setId(localUser, loginUserId);

            UUID nonExistentUserId = UUID.randomUUID();
            User nonExistentUser = new User();
            setId(nonExistentUser, nonExistentUserId);
            given(userRepository.findById(loginUserId)).willReturn(Optional.of(localUser));
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            UUID conversationId = UUID.randomUUID();
            DirectMessageSendRequest request = new DirectMessageSendRequest(
                    "test"
            );
            Conversation conversation = Conversation.builder()
                    .user(localUser)
                    .with(nonExistentUser)
                    .hasUnread(false)
                    .messages(null)
                    .build();
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

            //when&then
            assertThrows(UserNotFoundException.class,
                    ()-> directMessageService.saveDirectMessage(loginUserId,conversationId,request));
            verify(userRepository).findById(loginUserId);
            verify(userRepository).findById(nonExistentUserId);
            verify(conversationRepository).findById(conversationId);
            verify(directMessageRepository,never()).save(any(DirectMessage.class));
        }
    }

    @Nested
    @DisplayName("find()")
    class findDirectMessage {

        @Test
        @DisplayName("정상 요청 시 해당 채팅방의 DM 목록 조회함")
        void shouldGetDirectMessages() {
            //given
            DirectMessageSearchCond cond = new DirectMessageSearchCond();
            cond.setCursor(null);
            cond.setIdAfter(null);
            cond.setLimit(20);
            cond.setSortBy(SortBy.CREATED_AT);
            cond.setSortDirection(SortDirection.DESCENDING);

            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);
            UserSummary sender = new UserSummary(loginUserId, "test1", "test1");

            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);
            UserSummary receiver = new UserSummary(withUserId, "test2", "test2");
            UUID conversationId = UUID.randomUUID();

            Conversation conversation = Conversation.builder()
                    .user(loginUser)
                    .with(withUser)
                    .hasUnread(false)
                    .messages(null)
                    .build();

            given(conversationRepository.findById(conversationId))
                    .willReturn(Optional.of(conversation));

            DirectMessage directMessage = DirectMessage.builder()
                    .receiver(loginUser)
                    .sender(withUser)
                    .conversation(conversation)
                    .isRead(false)
                    .content("content")
                    .build();
            Pageable pageable = PageRequest.of(0, cond.getLimit() + 1);
            given(directMessageRepository.findFirstPage(any(UUID.class), any(Pageable.class)))
                    .willReturn(Arrays.asList(directMessage));
            given(directMessageMapper.toDirectMessageDto(directMessage))
                    .willReturn(new DirectMessageDto(
                            directMessage.getId(),
                            conversationId,
                            directMessage.getCreatedAt(),
                            sender,
                            receiver,
                            "content"
                            )
                    );
            given(directMessageRepository.countAllByConversationId(conversationId))
                    .willReturn(1L);
            //when
            CursorResponseDirectMessageDto result = directMessageService.getDirectMessages(loginUserId,conversationId,cond);
            //then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.data().get(0).id()).isEqualTo(directMessage.getId());
            assertThat(result.data().get(0).sender()).isEqualTo(sender);
            assertThat(result.data().get(0).receiver()).isEqualTo(receiver);
            assertThat(result.data().get(0).content()).isEqualTo("content");
        }

        @Test
        @DisplayName("해당 채팅방에 아무 DM도 없으면 빈 목록 반환함")
        void shouldReturnEmptyListWhenDirectMessageNotFound() {
            //given
            DirectMessageSearchCond cond = new DirectMessageSearchCond();
            cond.setCursor(null);
            cond.setIdAfter(null);
            cond.setLimit(20);
            cond.setSortBy(SortBy.CREATED_AT);
            cond.setSortDirection(SortDirection.DESCENDING);

            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);

            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);
            UUID conversationId = UUID.randomUUID();

            Conversation conversation = Conversation.builder()
                    .user(loginUser)
                    .with(withUser)
                    .hasUnread(false)
                    .messages(null)
                    .build();

            given(conversationRepository.findById(conversationId))
                    .willReturn(Optional.of(conversation));

            //when
            CursorResponseDirectMessageDto result = directMessageService.getDirectMessages(loginUserId,conversationId,cond);
            //then
            assertThat(result.totalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("cursor기준 이후 메세지만 반환함")
        void shouldGetMessagesAfterCursor() {
            //given
            DirectMessageSearchCond cond = new DirectMessageSearchCond();
            Instant fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0).toInstant(ZoneOffset.UTC);
            cond.setCursor(fixedTime.toString());
            cond.setIdAfter(null);
            cond.setLimit(20);
            cond.setSortBy(SortBy.CREATED_AT);
            cond.setSortDirection(SortDirection.DESCENDING);
            UUID conversationId = UUID.randomUUID();
            UUID after1Id = UUID.randomUUID();
            UUID after2Id = UUID.randomUUID();
            DirectMessage after1 = DirectMessage.builder()
                    .content("msg1")
                    .isRead(false)
                    .build();
            setId(after1, after1Id);

            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);
            UserSummary userA = new UserSummary(loginUserId, "test1", "test1");
            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);
            UserSummary userB = new UserSummary(withUserId, "test2", "test2");

            Conversation conversation = Conversation.builder()
                    .user(loginUser)
                    .with(withUser)
                    .hasUnread(false)
                    .messages(null)
                    .build();

            given(conversationRepository.findById(conversationId))
                    .willReturn(Optional.of(conversation));

            DirectMessage after2 = DirectMessage.builder()
                    .content("msg2")
                    .isRead(false)
                    .build();
            setId(after2, after2Id);
            given(directMessageRepository.findMessagesBefore(
                    any(UUID.class),
                    nullable(Instant.class),
                    nullable(UUID.class),
                    any(Pageable.class)
            )).willReturn(Arrays.asList(after1, after2));
            given(directMessageMapper.toDirectMessageDto(after1))
                    .willReturn(new DirectMessageDto(
                            after1Id,
                            conversationId,
                            after1.getCreatedAt(),
                            userA,
                            userB,
                            "msg1"
                    ));
            given(directMessageMapper.toDirectMessageDto(after2))
                    .willReturn(new DirectMessageDto(
                            after2Id,
                            conversationId,
                            after2.getCreatedAt(),
                            userB,
                            userA,
                            "msg2"
                    ));
            given(directMessageRepository.countAllByConversationId(conversationId))
            .willReturn(2L);
            //when
            CursorResponseDirectMessageDto result = directMessageService.getDirectMessages(loginUserId,conversationId,cond);
            //then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.data().get(0).id()).isEqualTo(after1.getId());
            assertThat(result.data().get(1).id()).isEqualTo(after2.getId());
        }


    }

    private static void setId(Object target, UUID id) {
        try {
            Field idField = BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID via reflection", e);
        }
    }
}
