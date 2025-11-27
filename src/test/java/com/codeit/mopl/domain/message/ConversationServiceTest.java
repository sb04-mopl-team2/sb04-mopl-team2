package com.codeit.mopl.domain.message;

import com.codeit.mopl.domain.base.BaseEntity;
import com.codeit.mopl.domain.message.conversation.dto.request.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.dto.response.ConversationDto;
import com.codeit.mopl.domain.message.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.entity.SortBy;
import com.codeit.mopl.domain.message.conversation.mapper.ConversationMapper;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.conversation.service.ConversationService;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.user.dto.response.UserSummary;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.exception.message.conversation.ConversationDuplicateException;
import com.codeit.mopl.exception.message.conversation.ConversationForbiddenException;
import com.codeit.mopl.exception.message.conversation.ConversationNotFound;
import com.codeit.mopl.exception.user.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

            UUID userA = loginUserId.compareTo(withUserId) < 0 ? loginUserId : withUserId;
            UUID userB = loginUserId.compareTo(withUserId) < 0 ? withUserId : loginUserId;

            given(userRepository.findById(loginUserId)).willReturn(Optional.of(loginUser));
            given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
            given(conversationRepository.findByUser_IdAndWith_Id(userA, userB))
                    .willReturn(Optional.empty());

            ConversationCreateRequest request =
                    new ConversationCreateRequest(withUserId);
            Conversation conversation = Conversation.builder()
                    .user(userA.equals(loginUserId) ? loginUser : withUser)
                    .with(userB.equals(loginUserId) ? loginUser : withUser)
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
            verify(conversationRepository).findByUser_IdAndWith_Id(userA, userB);
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
            given(userRepository.findById(loginUserId)).willReturn(Optional.of(loginUser));
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

            UUID userA = loginUserId.compareTo(withUserId) < 0 ? loginUserId : withUserId;
            UUID userB = loginUserId.compareTo(withUserId) < 0 ? withUserId : loginUserId;

            UUID existingConversationId = UUID.randomUUID();
            Conversation existingConversation = Conversation.builder()
                    .user(userA.equals(loginUserId) ? loginUser : withUser)
                    .with(userB.equals(loginUserId) ? loginUser : withUser)
                    .hasUnread(false)
                    .messages(new ArrayList<>())
                    .build();
            setId(existingConversation, existingConversationId);

            given(userRepository.findById(loginUserId)).willReturn(Optional.of(loginUser));
            given(userRepository.findById(withUserId)).willReturn(Optional.of(withUser));
            given(conversationRepository.findByUser_IdAndWith_Id(userA, userB))
                    .willReturn(Optional.of(existingConversation));
            ConversationCreateRequest request =
                    new ConversationCreateRequest(withUserId);

            //when &then
            assertThrows(ConversationDuplicateException.class,
                    () -> conversationService.createConversation(loginUserId,request));
            verify(userRepository).findById(loginUserId);
            verify(userRepository).findById(withUserId);
            verify(conversationRepository).findByUser_IdAndWith_Id(userA, userB);
            verify(conversationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("find()")
    class FindConversation {

        @Test
        @DisplayName("요청 파라미터 없이 모든 생성된 채팅방 목록을 조회함")
        void shouldFindConversations() {
            //given
            ConversationSearchCond cond = new ConversationSearchCond();
            cond.setKeywordLike(null);
            cond.setCursor(null);
            cond.setLimit(10);
            cond.setSortBy(SortBy.CREATED_AT);
            cond.setSortDirection(SortDirection.DESCENDING);

            UUID conversationId = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);
            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);
            UserSummary with = new UserSummary(withUserId, "test", "test" );

            Conversation conversation = Conversation.builder()
                    .user(loginUser)
                    .with(withUser)
                    .build();
            given(conversationRepository.findAllByCond(any(ConversationSearchCond.class)))
                    .willReturn(Arrays.asList(conversation));
            given(conversationMapper.toConversationDto(conversation,null))
                    .willReturn(new ConversationDto(
                            conversationId,
                            with,
                            null,
                            false
                    ));
            given(conversationRepository.countAllByCond(any(ConversationSearchCond.class)))
            .willReturn(1L);

            //when
            CursorResponseConversationDto result = conversationService.getAllConversations(loginUserId, cond);

            //then
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.data().get(0).id()).isEqualTo(conversationId);
            assertThat(result.data().get(0).with()).isEqualTo(with);
            assertThat(result.data().get(0).lastestMessage()).isEqualTo(null);
            assertThat(result.data().get(0).hasUnread()).isEqualTo(false);
        }

        @Test
        @DisplayName("해당 키워드가 유저네임 또는 메세지에 포함된 채팅방만 조회함")
        void shouldFindConversationsByKeyword() {
            //given
            ConversationSearchCond cond = new ConversationSearchCond();
            cond.setKeywordLike("test");
            cond.setCursor(null);
            cond.setLimit(10);
            cond.setSortBy(SortBy.CREATED_AT);
            cond.setSortDirection(SortDirection.DESCENDING);

            UUID loginUserId = UUID.randomUUID();

            UUID conversationId1 = UUID.randomUUID();
            UUID withUserId1 = UUID.randomUUID();
            User withUser1 = new User();
            setId(withUser1, withUserId1);
            UserSummary with1 = new UserSummary(withUserId1, "test", "test" );

            UUID conversationId2 = UUID.randomUUID();
            UUID withUserId2 = UUID.randomUUID();
            User withUser2 = new User();
            setId(withUser2, withUserId2);
            UserSummary with2 = new UserSummary(withUserId2, "test2", "test2" );

            Conversation conversation1 = Conversation.builder()
                    .user(withUser1)
                    .build();
            Conversation conversation2 = Conversation.builder()
                    .user(withUser2)
                    .build();
            List<Conversation> conversations = Arrays.asList(conversation1, conversation2);
            given(conversationRepository.findAllByCond(any(ConversationSearchCond.class)))
                    .willReturn(conversations);
            given(conversationMapper.toConversationDto(conversation1,null))
                    .willReturn(new ConversationDto(conversationId1,with1,null,true));
            given(conversationMapper.toConversationDto(conversation2,null))
                    .willReturn(new ConversationDto(conversationId2,with2,null,true));
            given(conversationRepository.countAllByCond(any(ConversationSearchCond.class)))
                    .willReturn(2L);

            //when
            CursorResponseConversationDto result = conversationService.getAllConversations(loginUserId, cond);

            //then
            assertThat(result.totalCount()).isEqualTo(2);
            assertThat(result.data().get(0).id()).isEqualTo(conversationId1);
            assertThat(result.data().get(1).id()).isEqualTo(conversationId2);
            assertThat(result.data().get(0).lastestMessage()).isEqualTo(null);
            assertThat(result.data().get(0).hasUnread()).isEqualTo(true);
        }

        @Test
        @DisplayName("키워드와 일치하는 결과가 없을 경우 빈리스트를 반환함")
        void shouldReturnEmptyListWhenKeywordNotFound() {
            //given
            ConversationSearchCond cond = new ConversationSearchCond();
            cond.setKeywordLike(null);
            cond.setCursor(null);
            cond.setLimit(10);
            cond.setSortBy(SortBy.CREATED_AT);
            cond.setSortDirection(SortDirection.DESCENDING);
            UUID loginUserId = UUID.randomUUID();
            given(conversationRepository.findAllByCond(any(ConversationSearchCond.class)))
                    .willReturn(Collections.emptyList());

            //when
            CursorResponseConversationDto result = conversationService.getAllConversations(loginUserId, cond);

            //then
            assertThat(result.totalCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("정상 요청 시 해당 채팅방의 기본 정보 조회함")
        void shouldFindConversationById() {
            //given
            UUID conversationId = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);

            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);
            UserSummary with = new UserSummary(withUserId, "test", "test" );
            Conversation conversation = new Conversation(
                    loginUser,
                    withUser,
                    true,
                    null
            );
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
            given(conversationMapper.toConversationDto(conversation,null))
            .willReturn(new ConversationDto(conversationId,with,null,true));

            //when
            ConversationDto result = conversationService.getConversationById(loginUserId,conversationId);

            //then
            assertThat(result.id()).isEqualTo(conversationId);
            assertThat(result.with()).isEqualTo(with);
        }

        @Test
        @DisplayName("채팅방이 존재하지 않을 경우 예외 발생")
        void shouldThrowExceptionWhenConversationNotFound() {
            // given
            UUID conversationId = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();

            given(conversationRepository.findById(conversationId)).willReturn(Optional.empty());

            // when & then
            assertThrows(ConversationNotFound.class,
                    () -> conversationService.getConversationById(loginUserId,conversationId));

            verify(conversationRepository).findById(conversationId);
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("정상 요청 시 특정 사용자와의 채팅방을 조회함")
        void shouldFindConversationWithUserId() {
            //given
            UUID conversationId = UUID.randomUUID();
            UUID loginUserId = UUID.randomUUID();
            User loginUser = new User();
            setId(loginUser, loginUserId);

            UUID withUserId = UUID.randomUUID();
            User withUser = new User();
            setId(withUser, withUserId);
            UserSummary with = new UserSummary(withUserId,"test", "test");

            Conversation conversation = Conversation.builder()
                    .user(loginUser)
                    .with(withUser)
                    .build();

            UUID userA = loginUserId.compareTo(withUserId) < 0 ? loginUserId : withUserId;
            UUID userB = loginUserId.compareTo(withUserId) < 0 ? withUserId : loginUserId;
            given(conversationRepository.findByUser_IdAndWith_Id(userA, userB))
                    .willReturn(Optional.of(conversation));
            given(conversationMapper.toConversationDto(conversation,null))
                    .willReturn(new ConversationDto(conversationId,with,null,true));
            // when
            ConversationDto result = conversationService.getConversationByUserId(loginUserId, withUserId);

            //then
            assertThat(result.id()).isEqualTo(conversationId);
            assertThat(result.with()).isEqualTo(with);
        }

        @Test
        @DisplayName("채팅방 조회 접근 권한 없을 경우 예외 발생")
        void shouldThrowExceptionWhenAccessDenied() {
            //given
            UUID conversationId = UUID.randomUUID();
            UUID deniedUserId = UUID.randomUUID(); // 권한 없는 사용자

            UUID loginUserId = UUID.randomUUID(); // 실제 대화 참여자 1
            User loginUser = new User();
            setId(loginUser, loginUserId);

            UUID withUserId = UUID.randomUUID(); // 실제 대화 참여자 2
            User withUser = new User();
            setId(withUser, withUserId);

            Conversation conversation = Conversation.builder()
                    .user(loginUser)  // 실제 참여자
                    .with(withUser)  // 실제 참여자
                    .build();
            setId(conversation, conversationId);

            // deniedUserId와 withUserId로 userA, userB 계산 (서비스 로직과 동일)
            UUID userA = deniedUserId.compareTo(withUserId) < 0 ? deniedUserId : withUserId;
            UUID userB = deniedUserId.compareTo(withUserId) < 0 ? withUserId : deniedUserId;

            // 대화방은 존재하지만, deniedUserId는 참여자가 아님
            given(conversationRepository.findByUser_IdAndWith_Id(userA, userB))
                    .willReturn(Optional.of(conversation)); // Optional.empty() → Optional.of()로 변경

            //when & then
            assertThrows(ConversationForbiddenException.class,
                    () -> conversationService.getConversationByUserId(deniedUserId, withUserId));
            verify(conversationRepository).findByUser_IdAndWith_Id(userA, userB);
            verify(conversationMapper, never()).toConversationDto(any(), any());
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
