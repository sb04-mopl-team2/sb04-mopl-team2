package com.codeit.mopl.domain.message.conversation.repository;

import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.entity.QConversation;
import com.codeit.mopl.domain.message.directmessage.entity.QDirectMessage;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.playlist.entity.SortBy;
import com.codeit.mopl.domain.user.entity.QUser;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.codeit.mopl.domain.message.conversation.entity.QConversation.conversation;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryImpl implements CustomConversationRepository {

    private final JPAQueryFactory query;


    @Override
    public List<Conversation> findAllByCond(ConversationSearchCond cond) {
        QConversation c = QConversation.conversation;
        QDirectMessage m = QDirectMessage.directMessage;
        QUser u = QUser.user;

        return query
                .selectDistinct(c)
                .from(c)
                .leftJoin(c.with, u)
                .leftJoin(c.messages, m)
                .where(
                        keywordLike(cond.getKeywordLike()),
                        cursorLessThan(cond.getCursor(), cond.getIdAfter())
                )
                .orderBy(buildOrderBy(cond.getSortBy(),cond.getSortDirection()))
                .limit(cond.getLimit() + 1)
                .fetch();
    }

    @Override
    public long countAllByCond(ConversationSearchCond cond) {
        return query
                .select(conversation.count())
                .from(conversation)
                .where(
                        keywordLike(cond.getKeywordLike())
                )
                .fetchCount();
    }

    private BooleanExpression keywordLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        return conversation.with.name.containsIgnoreCase(keyword)
                .or(QDirectMessage.directMessage.content.containsIgnoreCase(keyword));
    }

    private BooleanExpression cursorLessThan(String cursor, UUID idAfter) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
       LocalDateTime cursorCreatedAt = LocalDateTime.parse(cursor);
        BooleanExpression lessThan = conversation.createdAt.lt(cursorCreatedAt);

        if (idAfter == null) {
            return lessThan;
        }
        BooleanExpression tieBreaker = conversation.createdAt.eq(cursorCreatedAt)
                .and(conversation.id.lt(idAfter));
        return lessThan.or(tieBreaker);
    }

    private OrderSpecifier<?> buildOrderBy(SortBy sortBy, SortDirection sortDirection) {
        QConversation c = QConversation.conversation;
        boolean isDescending = sortDirection == SortDirection.DESCENDING;

        return isDescending ? c.createdAt.desc() : c.createdAt.asc();
        };
    }

