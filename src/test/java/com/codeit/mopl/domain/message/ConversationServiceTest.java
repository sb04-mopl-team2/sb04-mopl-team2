package com.codeit.mopl.domain.message;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.message.conversation.dto.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.ConversationDto;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.mapper.ConversationMapper;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.conversation.service.ConversationService;
import com.codeit.mopl.domain.user.dto.response.UserSummary;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.message.conversation.ConversationDuplicateException;
import com.codeit.mopl.exception.user.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConversationMapper conversationMapper;
    @InjectMocks private ConversationService conversationService;

    @Nested
    @DisplayName("create()")
    class CreateConversation {

        @Test
        @DisplayName("정상 요청 시 채팅방 생성됨")
        void shouldCreateConversation() {
            //given
            UUID conversationId = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);

            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);
            UserSummary with = new UserSummary(withUserId,"test", "test");

            given(userRepository.findById(loginUserId)).willReturn(Optional.of(withUser));
            given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
            given(conversationRepository.existsById(any())).willReturn(false);
            ConversationCreateRequest request =
                    new ConversationCreateRequest(withUserId);
            Conversation conversation = Conversation.builder()
                    .user(loginUser)
                    .with(withUser)
                    .hasUnread(false)
                    .messages(new ArrayList<>())
                    .build();
            ConversationDto dto = new ConversationDto(conversationId, with, null, false);
            given(conversationRepository.save(any(Conversation.class)))
                    .willReturn(conversation);
            given(conversationMapper.toConversationDto(conversation,null)).willReturn(dto);

            // when
            conversationService.createConversation(loginUserId,request);

            //then
            verify(userRepository).findById(loginUserId);
            verify(userRepository).findById(withUserId);
            verify(conversationRepository).existsById(any());
            verify(conversationRepository).save(any());
        }

        @Test
        @DisplayName("채팅 상대 유저가 존재하지 않을 경우 예외 발생")
        void shouldThrowExceptionWhenConversationUserNotFound() {
            //given
            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);

            UUID nonExistentUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, nonExistentUserId);
            given(userRepository.findById(loginUserId)).willReturn(Optional.of(withUser));
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            ConversationCreateRequest request = new ConversationCreateRequest(nonExistentUserId);

            //when & then
            assertThrows(UserNotFoundException.class,
                    () -> conversationService.createConversation(loginUserId,request));
            verify(userRepository).findById(loginUserId);
            verify(userRepository).findById(nonExistentUserId);
            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 생성된 채팅방일 경우 예외 발생 및 채팅방 생성되지 않음")
        void shouldThrowExceptionWhenConversationAlreadyExists() {
            //given
            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);

            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);

            given(userRepository.findById(loginUserId)).willReturn(Optional.of(withUser));
            given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
            given(conversationRepository.existsById(any())).willReturn(true);
            ConversationCreateRequest request =
                    new ConversationCreateRequest(withUserId);

            //when &then
            assertThrows(ConversationDuplicateException.class,
                    () -> conversationService.createConversation(loginUserId,request));
            verify(userRepository).findById(loginUserId);
            verify(userRepository).findById(withUserId);
            verify(conversationRepository, never()).save(any());
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
