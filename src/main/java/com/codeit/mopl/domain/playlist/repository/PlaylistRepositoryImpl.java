package com.codeit.mopl.domain.playlist.repository;

import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.playlist.dto.PlaylistSearchCond;
import com.codeit.mopl.domain.playlist.entity.Playlist;
import com.codeit.mopl.domain.playlist.entity.QPlaylist;
import com.codeit.mopl.domain.playlist.entity.SortBy;
import com.codeit.mopl.domain.playlist.subscription.QSubscription;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.DateTimeException;
import java.time.LocalDateTime;
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
                        subscriberEq(cond.getSubscriberIdEqual()),
                        cursorLessThan(cond.getCursor(), cond.getIdAfter())
                )
                .orderBy(buildOrderBy(cond.getSortBy(), cond.getSortDirection()))
                .limit(cond.getLimit() + 1 )
                .fetch();
    }

    @Override
    public long countAllByCond(PlaylistSearchCond cond) {
        return query
                .select(playlist.count())
                .from(playlist)
                .where(
                        keywordLike(cond.getKeywordLike()),
                        ownerEq(cond.getOwnerIdEqual()),
                        subscriberEq(cond.getSubscriberIdEqual())
                )
                .fetchOne();
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

    private BooleanExpression cursorLessThan(String cursor, UUID idAfter) {
        if (cursor == null) {
            return null;
        }
        LocalDateTime cursorCreatedAt;
        try {
            cursorCreatedAt = LocalDateTime.parse(cursor);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("올바르지 않은 커서 포맷" + cursor, e);
        }
        BooleanExpression lessThanCreatedAt = playlist.createdAt.lt(cursorCreatedAt);

        if (idAfter == null) {
            return lessThanCreatedAt;
        }
        BooleanExpression equalAndLtId = playlist.createdAt.eq(cursorCreatedAt)
                .and(playlist.id.lt(idAfter));
        return lessThanCreatedAt.or(equalAndLtId);
    }

    private OrderSpecifier<?> buildOrderBy(SortBy sortBy, SortDirection sortDirection) {
        QPlaylist playlist = QPlaylist.playlist;
        boolean isAscending = sortDirection == SortDirection.ASCENDING;

        return switch (sortBy) {
            case UPDATED_AT ->
                    isAscending ? playlist.updatedAt.asc() : playlist.updatedAt.desc();
            case SUBSCRIBER_COUNT ->
                isAscending ? playlist.subscriberCount.asc() : playlist.subscriberCount.desc();
            default -> playlist.createdAt.desc();
        };
    }
}
