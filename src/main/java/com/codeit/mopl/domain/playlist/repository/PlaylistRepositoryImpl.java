package com.codeit.mopl.domain.playlist.repository;

import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.entity.QPlaylist;
import com.codeit.mopl.domain.playlist.subscription.QSubscription;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.codeit.mopl.domain.playlist.entity.QPlaylist.playlist;

@Repository
@RequiredArgsConstructor
public class PlaylistRepositoryImpl implements CustomPlaylistRepository {

    private final JPAQueryFactory query;

    @Override
    public List<Playlist> findAllByCond(PlaylistSearchCond cond) {
        QPlaylist playlist = QPlaylist.playlist;

        return query.selectFrom(playlist)
                .where(
                        keywordLike(cond.getKeywordLike()),
                        ownerEq(cond.getOwnerIdEqual()),
                        subscriberEq(cond.getSubscriberIdEqual())
                )
                .orderBy(playlist.createdAt.desc())
                .limit(cond.getLimit())
                .fetch();
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        return playlist.title.containsIgnoreCase(keyword)
                .or(playlist.description.containsIgnoreCase(keyword));
    }

    private BooleanExpression ownerEq(UUID ownerId) {
        if (ownerId == null) {
            return null;
        }
        QPlaylist playlist = QPlaylist.playlist;
        return playlist.user.id.eq(ownerId);
    }

    private BooleanExpression subscriberEq(UUID subscriberId) {
        if (subscriberId == null) {
            return null;
        }
        QSubscription subscription = QSubscription.subscription;
        return playlist.id.in(
                query.select(subscription.playlist.id)
                        .from(subscription)
                        .where(subscription.subscriber.id.eq(subscriberId)));
    }
}
