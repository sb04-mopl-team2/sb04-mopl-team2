package com.codeit.mopl.domain.message.e2e;

import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.directmessage.dto.DirectMessageSendRequest;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.message.directmessage.repository.DirectMessageRepository;
import com.codeit.mopl.domain.message.e2e.config.TestWebSocketConfig;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.search.OpenSearchDataSync;
import com.codeit.mopl.search.service.OpenSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestWebSocketConfig.class)
public class DirectMessageE2ETest {

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private OpenSearchService openSearchService;

    @MockitoBean
    private OpenSearchClient openSearchClient;

    @MockitoBean
    private OpenSearchDataSync openSearchDataSync;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    ConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository directMessageRepository;

    @LocalServerPort
    private int port;

    WebSocketStompClient stompClient;

    private Conversation conversation;
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() throws Exception {
        directMessageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        sender = userRepository.save(
                new User("test@example.com", "test", "test")
        );

        receiver = userRepository.save(
                new User("receiver@example.com", "receiver", "receiver")
        );

        conversation = conversationRepository.save(
                Conversation.builder()
                        .user(sender)
                        .with(receiver)
                        .hasUnread(false)
                        .messages(new ArrayList<>())
                        .build()
        );

        stompClient = new WebSocketStompClient(
                new StandardWebSocketClient()
        );
        stompClient.setMessageConverter(
                new org.springframework.messaging.converter.MappingJackson2MessageConverter()
        );
    }

    @DisplayName("DM 전송 시 publish와 DB 저장이 정상적으로 동작한다")
    @Test
    void shouldPublishAndSaveDirectMessage() throws Exception {
        //given
        String sendEndpoint = "/pub/conversations/" + conversation.getId() + "/direct-messages";
        DirectMessageSendRequest messagePayload = new DirectMessageSendRequest("hello");

        // sender 세션만 생성 (구독은 하지 않음)
        StompSession senderSession = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        new StompHeaders() {{
                            add("X-TEST-USER", "sender");
                        }},
                        new StompSessionHandlerAdapter() {
                        }
                ).get(10, TimeUnit.SECONDS);

        // when: 메시지 전송 (publish 동작)
        senderSession.send(sendEndpoint, messagePayload);

        // then: DB에 저장되었는지 확인 (DM save 로직 검증)
        boolean messageSaved = false;
        for (int i = 0; i < 30; i++) {
            if (directMessageRepository.countAllByConversationId(conversation.getId()) > 0) {
                messageSaved = true;
                break;
            }
            Thread.sleep(100);
        }
        //then
        assertThat(messageSaved).isTrue();
        DirectMessage message = directMessageRepository.findAll().stream()
                .filter(dm -> dm.getConversation().getId().equals(conversation.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(message).isNotNull();
        assertThat(message.getContent()).isEqualTo("hello");
        assertThat(message.getSender().getId()).isEqualTo(sender.getId());
    }


    @DisplayName("존재하지 않는 채팅방(잘못된 destination)으로 DM 전송시 전송이 실패함")
    @Test
    void shouldNotReceiveMessageWhenConversationNotExists() throws Exception {

        // given
        UUID nonExistentConversationId = UUID.randomUUID();
        String sendEndpoint = "/pub/conversations/" + nonExistentConversationId + "/direct-messages";
        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        new StompHeaders() {{
                            add("X-TEST-USER", "sender");
                        }},
                        new StompSessionHandlerAdapter() {
                        }
                ).get(10, TimeUnit.SECONDS);
        //when
        session.send(sendEndpoint, new DirectMessageSendRequest("hello"));
        //then
        Thread.sleep(500);
        assertThat(directMessageRepository.countAllByConversationId(nonExistentConversationId)).isZero();
    }
}

