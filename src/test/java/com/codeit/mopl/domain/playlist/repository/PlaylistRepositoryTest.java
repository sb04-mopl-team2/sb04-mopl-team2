//package com.codeit.mopl.domain.playlist.repository;
//
//import com.codeit.mopl.domain.content.entity.Content;
//import com.codeit.mopl.domain.content.entity.ContentType;
//import com.codeit.mopl.domain.playlist.entity.Playlist;
//import com.codeit.mopl.domain.user.entity.User;
//import com.codeit.mopl.util.QueryDslConfig;
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//@DataJpaTest
//@Import(QueryDslConfig.class)
//@ActiveProfiles("test")
//public class PlaylistRepositoryTest {
//
//    @Autowired TestEntityManager em;
//
//    @Autowired PlaylistRepository playlistRepository;
//
//    private User user;
//    private Playlist playlist;
//    private Content content1;
//    private Content content2;
//
//    @BeforeEach
//    void init() {
//        user = new User("test@example.com", "test", "test");
//        em.persistAndFlush(user);
//
//        content1 = new Content();
//        content1.setTitle("test content1");
//        content1.setDescription("test content1");
//        content1.setContentType(ContentType.MOVIE);
//        em.persistAndFlush(content1);
//
//        content2 = new Content();
//        content2.setTitle("test content2");
//        content2.setDescription("test content2");
//        content2.setContentType(ContentType.SPORT);
//        em.persistAndFlush(content2);
//
//        playlist = new Playlist();
//        playlist.setUser(user);
//        playlist.setTitle("test playlist");
//        playlist.setDescription("test playlist");
//        em.persistAndFlush(playlist);
//        playlist.
//    }
//}
