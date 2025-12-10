package com.codeit.mopl.domain.message.repository;

import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.entity.SortBy;
import com.codeit.mopl.domain.message.conversation.repository.ConversationRepository;
import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.util.QueryDslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
@EnableJpaRepositories(
        basePackages = "com.codeit.mopl.domain.message.conversation.repository")
public class ConversationRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ConversationRepository conversationRepository;

    private User sender;
    private User receiver1;
    private User receiver2;
    private User receiver3;

    private Conversation conversation1;
    private Conversation conversation2;
    private Conversation conversation3;

    private DirectMessage directMessage;

    @BeforeEach
    void init() {
        sender = new User("sender@test.com","sender123","sender");
        em.persistAndFlush(sender);

        receiver1 = new User("receiver1.test.com","receiver111","receiver1");
        em.persistAndFlush(receiver1);

        receiver2 = new User("receiver2.test.com","receiver222","receiver2");
        em.persistAndFlush(receiver2);

        receiver3 = new User("receiver3.test.com","receiver333","receiver3");
        em.persistAndFlush(receiver3);

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

        conversation3 = Conversation.builder()
                .user(sender)
                .with(receiver3)
                .hasUnread(false)
                .messages(List.of())
                .build();
        em.persistAndFlush(conversation3);

        directMessage = DirectMessage.builder()
                .sender(sender)
                .receiver(receiver1)
                .conversation(conversation1)
                .content("hi")
                .isRead(false)
                .build();
        em.persistAndFlush(directMessage);

        em.clear();
    }

    @Test
    @DisplayName("기본 조회 - loginUser가 sender인 겨우 conversation1,2,3 모두 조회됨")
    void findConversationWhenLoginUserIsSender() {
        //given
        ConversationSearchCond cond = new ConversationSearchCond();
        cond.setLoginUserId(sender.getId());
        cond.setLimit(10);
        cond.setKeywordLike(null);
        cond.setSortDirection(SortDirection.DESCENDING);
        cond.setSortBy(SortBy.CREATED_AT);
        //when
        List<Conversation> result = conversationRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("기본 조회 - loginUser가 receiver1인 경우 conversation1만 조회됨")
    void findConversationWhenLoginUserIsReceiver1() {
        //given
        ConversationSearchCond cond = new ConversationSearchCond();
        cond.setLoginUserId(receiver1.getId());
        cond.setLimit(10);
        cond.setKeywordLike(null);
        cond.setSortDirection(SortDirection.DESCENDING);
        cond.setSortBy(SortBy.CREATED_AT);
        //when
        List<Conversation> result = conversationRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(conversation1.getId());
    }

    @Test
    @DisplayName("keywordLike - keyword가 null일 경우 요청자가 참여하는 모든 채팅방 조회됨")
    void findAllWhenKeywordLikeNull() {
        //given
        LocalDateTime t1 = LocalDateTime.now().minusDays(2);
        LocalDateTime t2 = LocalDateTime.now().minusDays(1);
        LocalDateTime t3 = LocalDateTime.now();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t1)
                .setParameter(2, conversation1.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t2)
                .setParameter(2, conversation2.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t3)
                .setParameter(2, conversation3.getId())
                .executeUpdate();

        em.flush();
        em.clear();

        ConversationSearchCond cond = new ConversationSearchCond();
        cond.setKeywordLike(null);
        cond.setLimit(10);
        cond.setLoginUserId(sender.getId());
        cond.setSortDirection(SortDirection.DESCENDING);
        cond.setSortBy(SortBy.CREATED_AT);
        //when
        List <Conversation> result = conversationRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getId()).isEqualTo(conversation3.getId());
    }

    @Test
    @DisplayName("keywordLike - 채팅방의 메세지나 상대방 닉네임에 keyword 포함 시 조회됨")
    void findAllByKeywordLike() {
        //given
        ConversationSearchCond cond = new ConversationSearchCond();
        cond.setKeywordLike("receiver1");
        cond.setLimit(10);
        cond.setLoginUserId(sender.getId());
        cond.setSortDirection(SortDirection.DESCENDING);
        cond.setSortBy(SortBy.CREATED_AT);

        //when
        List<Conversation> result = conversationRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getWith().getId()).isEqualTo(receiver1.getId());
    }

    @Test
    @DisplayName("cursorLessThan - cursor가 null일 경우 요청자가 참여하는 모든 채팅방 조회함")
    void findAllWhenCursorIsNull() {
        //given
        LocalDateTime t1 = LocalDateTime.now().minusDays(2);
        LocalDateTime t2 = LocalDateTime.now().minusDays(1);
        LocalDateTime t3 = LocalDateTime.now();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t1)
                .setParameter(2, conversation1.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t2)
                .setParameter(2, conversation2.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t3)
                .setParameter(2, conversation3.getId())
                .executeUpdate();

        em.flush();
        em.clear();

        ConversationSearchCond cond = new ConversationSearchCond();
        cond.setCursor(null);
        cond.setLimit(10);
        cond.setLoginUserId(sender.getId());
        cond.setSortDirection(SortDirection.DESCENDING);
        cond.setSortBy(SortBy.CREATED_AT);
        //when
        List<Conversation> result = conversationRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getWith().getId()).isEqualTo(receiver3.getId());
        assertThat(result.get(1).getWith().getId()).isEqualTo(receiver2.getId());
    }

    @Test
    @DisplayName("cursorLessThan - cursor 보다 createdAt값이 작은 채팅방만 조회함")
    void findAllWithCursorLessThan() {
        //given
        Instant t1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(1,ChronoUnit.DAYS);
        Instant t3 = Instant.now();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t1)
                .setParameter(2, conversation1.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t2)
                .setParameter(2, conversation2.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t3)
                .setParameter(2, conversation3.getId())
                .executeUpdate();

        em.flush();
        em.clear();

        ConversationSearchCond cond = new ConversationSearchCond();
        cond.setCursor(t2.toString());
        cond.setLimit(1);
        cond.setLoginUserId(sender.getId());
        cond.setSortDirection(SortDirection.DESCENDING);
        cond.setSortBy(SortBy.CREATED_AT);
        //when
        List<Conversation> result = conversationRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getWith().getId()).isEqualTo(receiver1.getId());
        assertThat(result.get(0).getCreatedAt()).isEqualTo(t1);
    }

    @Test
    @DisplayName("hasNext - hasNext == true 일 경우, cursor 기반 페이지네이션 동작이 정상적으로 동작함")
    void findAllWhenHasNext() {
        //given & when : createdAt 강제 세팅
        Instant t1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(1,ChronoUnit.DAYS);
        Instant t3 = Instant.now();
        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t1)
                .setParameter(2, conversation1.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t2)
                .setParameter(2, conversation2.getId())
                .executeUpdate();

        em.getEntityManager()
                .createNativeQuery("UPDATE conversations SET created_at = ? WHERE id = ?")
                .setParameter(1, t3)
                .setParameter(2, conversation3.getId())
                .executeUpdate();
        em.flush();
        em.clear();
        // 첫 페이지 조회
        ConversationSearchCond cond1 = new ConversationSearchCond();
        cond1.setLimit(2);
        cond1.setLoginUserId(sender.getId());
        cond1.setSortBy(SortBy.CREATED_AT);
        cond1.setSortDirection(SortDirection.DESCENDING);
        List<Conversation> page1 = conversationRepository.findAllByCond(cond1);
        // 두 번째 페이지 조회
        Conversation last = page1.get(1);
        ConversationSearchCond cond2 = new ConversationSearchCond();
        cond2.setCursor(t2.toString());
        cond2.setIdAfter(last.getId());
        cond2.setLimit(1);
        cond2.setLoginUserId(sender.getId());
        cond2.setSortBy(SortBy.CREATED_AT);
        cond2.setSortDirection(SortDirection.DESCENDING);
        List<Conversation> page2 = conversationRepository.findAllByCond(cond2);
        //then
        assertThat(page1.size()).isEqualTo(3);
        assertThat(page2.size()).isEqualTo(1);
        assertThat(page1.get(0).getWith().getId()).isEqualTo(receiver3.getId());
        assertThat(page1.get(1).getCreatedAt()).isEqualTo(t2);
        assertThat(page2.get(0).getWith().getId()).isEqualTo(receiver1.getId());
    }

    @Test
    @DisplayName("정렬 - CREATED_AT DESC로 정렬됨")
    void orderByCreatedAtDesc() {
         //given
        Instant t1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(1,ChronoUnit.DAYS);
        Instant t3 = Instant.now();
         ReflectionTestUtils.setField(conversation1, "createdAt", t1);
         ReflectionTestUtils.setField(conversation2, "createdAt", t2);
         ReflectionTestUtils.setField(conversation3, "createdAt", t3);
         em.flush();
         em.clear();
         ConversationSearchCond cond = new ConversationSearchCond();
         cond.setSortBy(SortBy.CREATED_AT);
         cond.setSortDirection(SortDirection.DESCENDING);
         cond.setLimit(10);
         cond.setLoginUserId(sender.getId());
         //when
        List<Conversation> result = conversationRepository.findAllByCond(cond);
        //the
        assertThat(result.size()).isEqualTo(3);
        assertThat(result).isSortedAccordingTo(
                (a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt())
        );
    }
}
