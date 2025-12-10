package com.codeit.mopl.domain.message.repository;

import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.message.directmessage.repository.DirectMessageRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.util.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
@EnableJpaRepositories(
        basePackages = "com.codeit.mopl.domain.message.directmessage.repository"
)
public class DirectMessageRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    DirectMessageRepository directMessageRepository;

    private User sender;
    private User receiver1;
    private User receiver2;

    private DirectMessage directMessage1;
    private DirectMessage directMessage2;
    private DirectMessage directMessage3;

    private Conversation conversation1;
    private Conversation conversation2;

    @BeforeEach
    void init() {
        sender = new User("sender@test.com","sender123","sender");
        em.persistAndFlush(sender);

        receiver1 = new User("receiver1.test.com","receiver111","receiver1");
        em.persistAndFlush(receiver1);

        receiver2 = new User("receiver2.test.com","receiver222","receiver2");
        em.persistAndFlush(receiver2);

        conversation1 = Conversation.builder()
                .user(sender)
                .with(receiver1)
                .hasUnread(true)
                .messages(List.of())
                .build();
        em.persistAndFlush(conversation1);

        conversation2 = Conversation.builder()
                .user(sender)
                .with(receiver2)
                .hasUnread(false)
                .messages(List.of())
                .build();
        em.persistAndFlush(conversation2);

        directMessage1 = DirectMessage.builder()
                .sender(sender)
                .receiver(receiver1)
                .conversation(conversation1)
                .content("hi")
                .isRead(true)
                .build();
        em.persistAndFlush(directMessage1);

        directMessage2 = DirectMessage.builder()
                .sender(receiver1)
                .receiver(sender)
                .conversation(conversation1)
                .content("hello")
                .isRead(false)
                .build();
        em.persistAndFlush(directMessage2);

        directMessage3 = DirectMessage.builder()
                .sender(sender)
                .receiver(receiver2)
                .conversation(conversation2)
                .content("hey")
                .isRead(false)
                .build();
        em.persistAndFlush(directMessage3);

        em.clear();
    }

    @Test
    @DisplayName("조회 - 첫 번째 페이지 조회")
    void findFirstPage() {
        //given
        Pageable pageable = Pageable.ofSize(2);
        LocalDateTime t1 = LocalDateTime.now().minusDays(3);
        LocalDateTime t2 = LocalDateTime.now().minusDays(2);

        em.getEntityManager()
                .createNativeQuery("UPDATE direct_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, t1)
                .setParameter(2, directMessage1.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE direct_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, t2)
                .setParameter(2, directMessage2.getId())
                .executeUpdate();

        em.flush();
        em.clear();

        //when
        List<DirectMessage> result = directMessageRepository.findFirstPage(conversation1.getId(), pageable);
        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(directMessage2.getId());
        assertThat(result.get(1).getId()).isEqualTo(directMessage1.getId());
    }

    @Test
    @DisplayName("조회 - 커서 이전 페이지 조회")
    void findMessagesBefore() {
        //given
        UUID idAfter = directMessage2.getId();
        LocalDateTime t1 = LocalDateTime.now().minusDays(3);
        LocalDateTime t2 = LocalDateTime.now().minusDays(2);
        LocalDateTime t3 = LocalDateTime.now().minusDays(1);

        em.getEntityManager()
                .createNativeQuery("UPDATE direct_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, t1)
                .setParameter(2, directMessage1.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE direct_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, t2)
                .setParameter(2, directMessage2.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE direct_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, t3)
                .setParameter(2, directMessage3.getId())
                .executeUpdate();
        em.flush();
        em.clear();

        Pageable pageable = Pageable.ofSize(2);

        DirectMessage dm2 = em.find(DirectMessage.class, directMessage2.getId());
        LocalDateTime cursor = dm2.getCreatedAt();

        //when
        List<DirectMessage> result = directMessageRepository.findMessagesBefore(
                conversation1.getId(),
                cursor,
                idAfter,
                pageable
        );
        //then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(directMessage1.getId());
    }

    @Test
    @DisplayName("커서 이후 페이지 조회")
    void findMessagesAfter() {
        //given
        UUID idAfter = directMessage1.getId();
        LocalDateTime t1 = LocalDateTime.now().minusDays(3);
        LocalDateTime t2 = LocalDateTime.now().minusDays(2);

        em.getEntityManager()
                .createNativeQuery("UPDATE direct_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, t1)
                .setParameter(2, directMessage1.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE direct_messages SET created_at = ? WHERE id = ?")
                .setParameter(1, t2)
                .setParameter(2, directMessage2.getId())
                .executeUpdate();

        em.flush();
        em.clear();

        Pageable pageable = Pageable.ofSize(3);
        DirectMessage dm1 = em.find(DirectMessage.class, idAfter);
        LocalDateTime cursor = dm1.getCreatedAt();
        //when
        List<DirectMessage> result = directMessageRepository.findMessagesAfter(
                conversation1.getId(),
                cursor,
                idAfter,
                pageable
        );
        //then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(directMessage2.getId());
    }

    @Test
    @DisplayName("채팅방에 isRead == false인 메세지가 존재하는 경우 true를 반환함")
    void existsByConversationIdAndReceiverIdAndIsReadFalse_ReturnsTrue_WhenUnreadMessageExists() {
        //given
        UUID conversationId = conversation2.getId();
        UUID receiverId = receiver2.getId();
        //when
        boolean result = directMessageRepository.existsByConversationIdAndReceiverIdAndIsReadFalse(conversationId,receiverId);
        //then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("채팅방의 총 DM 개수를 정확하게 계산하여 반환함")
    void countAllByConversationId() {
        //given
        UUID conversationId = conversation1.getId();
        //when
        long count = directMessageRepository.countAllByConversationId(conversationId);
        //then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("정렬 - CREATED_AT DESC로 정렬됨")
    void orderByCreatedAtDesc() {
        //given & when
        List<DirectMessage> page = directMessageRepository.findFirstPage(conversation1.getId(), Pageable.ofSize(10));
        //then
        assertThat(page).isSortedAccordingTo(
                (a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())
        );
    }
}
