package com.codeit.mopl.domain.playlist.repository;

import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.entity.SortBy;
import com.codeit.mopl.domain.playlist.playlistitem.entity.PlaylistItem;
import com.codeit.mopl.domain.playlist.subscription.entity.Subscription;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QueryDslConfig.class)
@ActiveProfiles("test")
@EnableJpaRepositories(
        basePackages = "com.codeit.mopl.domain.playlist.repository")
public class PlaylistRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired PlaylistRepository playlistRepository;

    private User user;
    private Playlist playlist1;
    private Playlist playlist2;
    private Playlist playlist3;
    private Content content1;
    private Content content2;

    @BeforeEach
    void init() {
        user = new User("test@example.com", "test", "test");
        em.persistAndFlush(user);

        content1 = new Content();
        content1.setTitle("test content1");
        content1.setDescription("test content1");
        content1.setContentType(ContentType.MOVIE);
        em.persistAndFlush(content1);

        content2 = new Content();
        content2.setTitle("test content2");
        content2.setDescription("test content2");
        content2.setContentType(ContentType.SPORT);
        em.persistAndFlush(content2);

        playlist1 = new Playlist();
        playlist1.setUser(user);
        playlist1.setTitle("test playlist");
        playlist1.setDescription("test playlist");
        em.persistAndFlush(playlist1);

        playlist2 = new Playlist();
        playlist2.setUser(user);
        playlist2.setTitle("test playlist2");
        playlist2.setDescription("test playlist2");
        em.persistAndFlush(playlist2);

        playlist3 = new Playlist();
        playlist3.setUser(user);
        playlist3.setTitle("test playlist3");
        playlist3.setDescription("test playlist3");
        em.persistAndFlush(playlist3);

        PlaylistItem item1 = new PlaylistItem(playlist1, content1);
        PlaylistItem item2 = new PlaylistItem(playlist1, content2);
        em.persistAndFlush(item1);
        em.persistAndFlush(item2);

        // 1차 캐시 비우기
        em.clear();
    }

    @Test
    @DisplayName("keywordLike - 플레이리스트 제목과 설명에 keyword 포함 시 조회됨")
    void findAllByKeywordLike() {
        //given
        PlaylistSearchCond cond = new PlaylistSearchCond();
        cond.setKeywordLike("playlist");
        cond.setLimit(10);
        //when
        List<Playlist> result = playlistRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getTitle()).isEqualTo("test playlist3");
    }

    @Test
    @DisplayName("ownerEq - 특정 유저가 소유한 playlist만 조회함")
    void findAllByOwnerEq() {
        //given
        PlaylistSearchCond cond = new PlaylistSearchCond();
        cond.setOwnerIdEqual(user.getId());
        cond.setLimit(10);
        //when
        List<Playlist> result = playlistRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(0).getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("subscriberEq - 특정 유저가 구독한 플레이리스트 조회함")
    void findAllBySubscriberEq() {
        //given
        Subscription subscription = new Subscription(playlist1,user, LocalDateTime.now());
        em.persistAndFlush(subscription);
        em.clear();

        PlaylistSearchCond cond = new PlaylistSearchCond();
        cond.setSubscriberIdEqual(user.getId());
        cond.setLimit(10);
        //when
        List<Playlist> result = playlistRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(playlist1.getId());
    }

    @Test
    @DisplayName("cursorLessThan - cursor 보다 createdAt이 과거인 playlist만 조회함")
    void findAllWithCursorLessThan() {
        //given
        //createdAt을 강제로 세팅해줌
        ReflectionTestUtils.setField(playlist1, "createdAt", LocalDateTime.now().minusDays(1));
        em.flush();
        em.clear();

        PlaylistSearchCond cond = new PlaylistSearchCond();
        cond.setCursor(LocalDateTime.now().toString());
        cond.setLimit(10);
        //when
        List<Playlist> result = playlistRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Cursor - hasNext == true 일 경우, cursor 기반 페이지네이션 동작이 정상적으로 동작함")
    void hasNext() {
        //given & when
        LocalDateTime t1 = LocalDateTime.now().minusDays(3);
        LocalDateTime t2 = LocalDateTime.now().minusDays(2);
        LocalDateTime t3 = LocalDateTime.now().minusDays(1);

        Playlist p1 = em.find(Playlist.class, playlist1.getId());
        Playlist p2 = em.find(Playlist.class, playlist2.getId());
        Playlist p3 = em.find(Playlist.class, playlist3.getId());

        ReflectionTestUtils.setField(p1, "createdAt", t1);
        ReflectionTestUtils.setField(p2, "createdAt", t2);
        ReflectionTestUtils.setField(p3, "createdAt", t3);
        em.flush();
        em.clear();

        // 첫 페이지 조회 (limit = 1)
        PlaylistSearchCond cond = new PlaylistSearchCond();
        cond.setLimit(1);
        cond.setSortBy(SortBy.UPDATED_AT);
        cond.setSortDirection(SortDirection.DESCENDING);
        List<Playlist> page1 = playlistRepository.findAllByCond(cond);

        //두 번째 페이지 조회
        Playlist last = page1.get(1);
        PlaylistSearchCond cond2 = new PlaylistSearchCond();
        cond2.setLimit(2);
        cond2.setSortBy(SortBy.UPDATED_AT);
        cond2.setSortDirection(SortDirection.DESCENDING);
        cond2.setCursor(last.getCreatedAt().toString());
        cond2.setIdAfter(last.getId());
        List<Playlist> page2 = playlistRepository.findAllByCond(cond2);

        //then
        assertThat(page1.size()).isEqualTo(2);
        assertThat(page1.get(0).getId()).isEqualTo(p3.getId());
        assertThat(page1.get(1).getId()).isEqualTo(p2.getId());
        assertThat(page2.size()).isEqualTo(1);
        assertThat(page2.get(0).getId()).isEqualTo(p1.getId());
    }

    @Test
    @DisplayName("정렬 - UPDATED_AT desc로 정렬됨")
    void orderByUpdatedAtDesc() {
        //given
        LocalDateTime t1 = LocalDateTime.now().minusDays(2);
        LocalDateTime t2 = LocalDateTime.now().minusDays(1);
        LocalDateTime t3 = LocalDateTime.now();

        Playlist p1 = em.find(Playlist.class, playlist1.getId());
        Playlist p2 = em.find(Playlist.class, playlist2.getId());
        Playlist p3 = em.find(Playlist.class, playlist3.getId());

        ReflectionTestUtils.setField(p1,"updatedAt",t1);
        ReflectionTestUtils.setField(p2,"updatedAt",t2);
        ReflectionTestUtils.setField(p3,"updatedAt",t3);

        em.flush();
        em.clear();

        PlaylistSearchCond cond = new PlaylistSearchCond();
        cond.setSortBy(SortBy.UPDATED_AT);
        cond.setSortDirection(SortDirection.DESCENDING);
        cond.setLimit(10);
        //when
        List<Playlist> result = playlistRepository.findAllByCond(cond);
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result).isSortedAccordingTo(
                (a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt())
        );
    }
}
