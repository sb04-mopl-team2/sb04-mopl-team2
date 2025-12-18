package com.codeit.mopl.domain.message.conversation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codeit.mopl.domain.base.SortBy;
import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.message.conversation.dto.request.ConversationCreateRequest;
import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.dto.response.ConversationDto;
import com.codeit.mopl.domain.message.conversation.dto.response.CursorResponseConversationDto;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.conversation.service.ConversationService;
import com.codeit.mopl.domain.message.directmessage.dto.CursorResponseDirectMessageDto;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSearchCond;
import com.codeit.mopl.domain.message.directmessage.repository.DirectMessageRepository;
import com.codeit.mopl.domain.message.directmessage.service.DirectMessageService;
import com.codeit.mopl.domain.user.dto.response.UserSummary;
import com.codeit.mopl.domain.user.mapper.UserMapper;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.domain.user.service.UserService;
import com.codeit.mopl.exception.message.conversation.ConversationDuplicateException;
import com.codeit.mopl.exception.message.conversation.ConversationForbiddenException;
import com.codeit.mopl.exception.message.directmessage.DirectMessageNotFound;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import com.codeit.mopl.oauth.service.OAuth2UserService;
import com.codeit.mopl.security.CustomUserDetailsService;
import com.codeit.mopl.security.jwt.filter.JwtAuthenticationFilter;
import com.codeit.mopl.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.codeit.mopl.security.jwt.handler.OAuth2UserSuccessHandler;
import com.codeit.mopl.security.jwt.provider.JwtTokenProvider;
import com.codeit.mopl.security.jwt.registry.JwtRegistry;
import com.codeit.mopl.util.WithCustomMockUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = ConversationController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private DirectMessageService directMessageService;

    @MockitoBean
    private DirectMessageRepository directMessageRepository;

    @MockitoBean
    private ConversationRepository conversationRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private OAuth2UserService oAuth2UserService;

    @MockitoBean
    private OAuth2UserSuccessHandler oAuth2UserSuccessHandler;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    /**
     * 채팅방 생성
     */
    @WithCustomMockUser
    @Test
    @DisplayName("채팅방 생성 - 채팅방이 생성됨")
    void createConversationSuccess() throws Exception {
        //given
        UUID withUserId = UUID.randomUUID();
        UserSummary withUser = new UserSummary(UUID.randomUUID(), "username", "profileImage");
        UserSummary user = new UserSummary(UUID.randomUUID(), "username2", "profileImage2");
        ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
        UUID conversationId = UUID.randomUUID();
        ConversationDto response = new ConversationDto(
                conversationId, withUser, null, false);
        when(conversationService.createConversation(any(UUID.class),any(ConversationCreateRequest.class)))
                .thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                post("/api/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
        //then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("채팅방 생성 - 채팅 상대 유저가 존재하지 않을 시 404예외 발생함")
    void createConversationFailWithUserNotFound() throws Exception {
        //given
        UUID nonExistentWithUserId = UUID.randomUUID();
        ConversationCreateRequest request = new ConversationCreateRequest(nonExistentWithUserId);
        when(conversationService.createConversation(any(UUID.class),any(ConversationCreateRequest.class)))
                .thenThrow(new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", nonExistentWithUserId.toString())));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
        //then
        result.andExpect(status().isNotFound());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("채팅방 생성 - 이미 생성된 채팅방일 시 409예외 발생함")
    void createConversationFailWhenConversationAlreadyExists() throws Exception {
        //given
        UUID withUserId = UUID.randomUUID();
        ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
        when(conversationService.createConversation(any(UUID.class),any(ConversationCreateRequest.class)))
                .thenThrow(ConversationDuplicateException.withId(withUserId));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
        //then
        result.andExpect(status().isConflict());
    }


    /**
     * 채팅방 목록 조회(커서 페이지네이션)
     */
    @WithCustomMockUser
    @Test
    @DisplayName("채팅방 목록 조회 - keyword 없이 로그인 유저가 참여하는 전체 채팅방 목록을 조회함")
    void getConversations() throws Exception {
        //given
        UUID withUserId = UUID.randomUUID();
        ConversationSearchCond cond = new ConversationSearchCond();
        cond.setLoginUserId(withUserId);
        cond.setKeywordLike(null);
        cond.setLimit(10);
        cond.setSortBy(SortBy.CREATED_AT);
        cond.setSortDirection(SortDirection.DESCENDING);
        CursorResponseConversationDto response = new CursorResponseConversationDto(
                List.of(), null, null, false, 5L, SortBy.CREATED_AT, SortDirection.DESCENDING
        );
        when(conversationService.getAllConversations(any(UUID.class), any(ConversationSearchCond.class)))
                .thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/conversations")
                        .param("limit", "10")
                        .param("sortBy", "CREATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("채팅방 목록 조회 - keyword 검색을 적용하여 조회함")
    void getConversationsWithKeyword() throws Exception {
        //given
        CursorResponseConversationDto response = new CursorResponseConversationDto(
                List.of(), null, null, false, 5L, SortBy.CREATED_AT, SortDirection.DESCENDING
        );
        when(conversationService.getAllConversations(any(UUID.class), any(ConversationSearchCond.class)))
        .thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/conversations")
                        .param("keywordLike", "test")
                        .param("limit", "10")
                        .param("sortBy", "CREATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").isNumber());
    }

    /**
     * 채팅방 정보 조회
     */
    @WithCustomMockUser
    @Test
    @DisplayName("채팅방 정보 조회 - 선택한 유저와의 채팅방을 조회함")
    void getConversationWithSpecificUserId() throws Exception {
        //given
        UUID conversationId = UUID.randomUUID();
        ConversationDto response = new ConversationDto(
                conversationId, null, null, false);
        when(conversationService.getConversationById(any(UUID.class), any()))
                .thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/conversations/{conversationId}", conversationId)
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.latestMessage").isEmpty())
                .andExpect(jsonPath("$.hasUnread").value(false));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("채팅방 정보 조회 - 요청자가 해당 채팅방 참여자가 아니라면 403예외 발생함")
    void getConversationWithUnauthorizedUser() throws Exception {
        //given
        UUID conversationId = UUID.randomUUID();
        UUID loginUserId = UUID.randomUUID();
        when(conversationService.getConversationById(any(UUID.class), any()))
                .thenThrow(ConversationForbiddenException.withId(loginUserId));
        //when
        ResultActions result = mockMvc.perform(
                get("/api/conversations/{conversationId}", conversationId)
        );
        //then
        result.andExpect(status().isForbidden());
    }

    /**
     * DM 읽음 처리
     */
    @WithCustomMockUser
    @Test
    @DisplayName("DM 읽음 처리 - DM을 읽음 처리함")
    void markMessageAsRead() throws Exception {
        //given
        UUID loginUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        doNothing()
                .when(conversationService)
                .markAsRead(loginUserId, conversationId, directMessageId);
        //when
        ResultActions result = mockMvc.perform(
                post("/api/conversations/{conversationId}/direct-messages/{directMessageId}/read", conversationId, directMessageId)
        );
        //then
        result.andExpect(status().isOk());
    }

    @WithCustomMockUser
    @Test
    @DisplayName("DM 읽음 처리 - DM이 해당 채팅방에 속하지 않을 경우 404예외 발생함")
    void markMessageAsReadFailWhenMessageNotFound () throws Exception {
        //given
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        doThrow(DirectMessageNotFound.withId(directMessageId))
                .when(conversationService)
                .markAsRead(any(UUID.class), any(UUID.class), any(UUID.class));
        //when
        ResultActions result = mockMvc.perform(
                post("/api/conversations/{conversationId}/direct-messages/{directMessageId}/read", conversationId, directMessageId)
        );
        //then
        result.andExpect(status().isNotFound());
    }

    /**
     * DM 목록 조회(커서 페이지네이션)
     */
    @WithCustomMockUser
    @Test
    @DisplayName("DM 목록 조회 - DM 목록을 조회함")
    void getDirectMessages() throws Exception {
        //given
        UUID loginUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        DirectMessageSearchCond cond = new DirectMessageSearchCond();
        cond.setLimit(10);
        cond.setSortBy(SortBy.CREATED_AT);
        cond.setSortDirection(SortDirection.DESCENDING);

        CursorResponseDirectMessageDto response = new CursorResponseDirectMessageDto(
                List.of(), null, null, false, 5L, SortBy.CREATED_AT, SortDirection.DESCENDING
        );
        when(directMessageService.getDirectMessages(any(UUID.class),any(UUID.class),any(DirectMessageSearchCond.class)))
                .thenReturn(response);
        //when
        ResultActions result = mockMvc.perform(
                get("/api/conversations/{conversationId}/direct-messages", conversationId)
                        .param("limit", "10")
                        .param("sortBy", "CREATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("DM 목록 조회 - cursor 포함하여 다음페이지를 조회함")
    void getDirectMessagesWithCursor() throws Exception {
        //given
        UUID conversationId = UUID.randomUUID();
        UUID directMessageId = UUID.randomUUID();
        DirectMessageSearchCond cond = new DirectMessageSearchCond();
        cond.setCursor("cursor123");
        cond.setIdAfter(directMessageId);

        cond.setLimit(10);
        cond.setSortBy(SortBy.CREATED_AT);
        cond.setSortDirection(SortDirection.DESCENDING);

        CursorResponseDirectMessageDto response = new CursorResponseDirectMessageDto(
                List.of(), "cursor123", directMessageId, true, 20L, SortBy.CREATED_AT, SortDirection.DESCENDING
        );
        when(directMessageService.getDirectMessages(any(UUID.class),any(UUID.class),any(DirectMessageSearchCond.class)))
                .thenReturn(response);
        //then
        ResultActions result = mockMvc.perform(
                get("/api/conversations/{conversationId}/direct-messages", conversationId)
                        .param("cursor", "cursor123")
                        .param("idAfter", directMessageId.toString())
                        .param("limit", "10")
                        .param("sortBy", "CREATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").value("cursor123"))
                .andExpect(jsonPath("$.nextIdAfter").value(directMessageId.toString()));
    }

    @WithCustomMockUser
    @Test
    @DisplayName("DM 목록 조회 - 로그인 유저가 채팅방 참여자가 아니라면 403예외 반환함")
    void getDirectMessagesFailForbidden() throws Exception {
        //given
        UUID loginUserId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(directMessageService.getDirectMessages(any(),any(),any()))
                .thenThrow(ConversationForbiddenException.withId(loginUserId));
        //when
        ResultActions result = mockMvc.perform(
                get("/api/conversations/{conversationId}/direct-messages", conversationId)
                        .param("limit", "10")
                        .param("sortBy", "CREATED_AT")
                        .param("sortDirection", "DESCENDING")
        );
        //then
        result.andExpect(status().isForbidden());
    }
}
