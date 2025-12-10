package com.codeit.mopl.domain.message.conversation.repository;

import com.codeit.mopl.domain.message.conversation.dto.request.ConversationSearchCond;
import com.codeit.mopl.domain.message.conversation.entity.Conversation;
import com.codeit.mopl.domain.message.conversation.entity.QConversation;
import com.codeit.mopl.domain.message.conversation.entity.SortBy;
import com.codeit.mopl.domain.message.directmessage.entity.QDirectMessage;
import com.codeit.mopl.domain.notification.entity.SortDirection;
import com.codeit.mopl.domain.user.entity.QUser;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.format.DateTimeParseException;
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
        QUser u1 = new QUser("userA");
        QUser u2 = new QUser("userB");
        UUID loginUserId = cond.getLoginUserId();

        return query
                .selectDistinct(c)
                .from(c)
                .leftJoin(c.user, u1)
                .leftJoin(c.with, u2)
                .leftJoin(c.messages, m)
                .where(
                        c.user.id.eq(loginUserId).or(c.with.id.eq(loginUserId)),
                        keywordLike(cond.getKeywordLike(), loginUserId, u1, u2, m),
                        cursorLessThan(cond.getCursor(), cond.getIdAfter())
                )
                .orderBy(buildOrderBy(cond.getSortBy(),cond.getSortDirection()))
                .limit(cond.getLimit() + 1)
                .fetch();
    }

    @Override
    public long countAllByCond(ConversationSearchCond cond) {
        QConversation c = QConversation.conversation;
        QDirectMessage m = QDirectMessage.directMessage;
        QUser u1 = new QUser("user1");
        QUser u2 = new QUser("user2");
        UUID loginUserId = cond.getLoginUserId();
        return query
                .select(c.count())
                .from(c)
                .leftJoin(c.user,u1)
                .leftJoin(c.with,u2)
                .leftJoin(c.messages,m)
                .where(
                        c.user.id.eq(loginUserId).or(c.with.id.eq(loginUserId)),
                        keywordLike(cond.getKeywordLike(),loginUserId,u1,u2,m)
                )
                .fetchOne();
    }

    private BooleanExpression keywordLike(
            String keyword,
            UUID loginUserId,
            QUser u1,
            QUser u2,
            QDirectMessage m
    ) {
        QConversation c = QConversation.conversation;
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        BooleanExpression WithNameContains =
                // 본인이 loginUser인 경우 -> with의 이름
                c.user.id.eq(loginUserId).and(u2.name.containsIgnoreCase(keyword))
                    .or(
                        //내가 with인 경우 -> loginUser의 이름
                        c.with.id.eq(loginUserId).and(u1.name.containsIgnoreCase(keyword))
                        );
        BooleanExpression messageContains = m.content.containsIgnoreCase(keyword);
        return WithNameContains.or(messageContains);
    }

    private BooleanExpression cursorLessThan(String cursor, UUID idAfter) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
       Instant cursorInstant;

        try {
          cursorInstant = Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            return null;
        }

        BooleanExpression ltCursor = conversation.createdAt.lt(cursorInstant);

        if (idAfter == null) {
            return ltCursor;
        }
        BooleanExpression tieBreaker = conversation.createdAt.eq(cursorInstant)
                .and(conversation.id.lt(idAfter));
        return ltCursor.or(tieBreaker);
    }

    private OrderSpecifier<?> buildOrderBy(SortBy sortBy, SortDirection sortDirection) {
        QConversation c = QConversation.conversation;
        boolean isDescending = sortDirection == SortDirection.DESCENDING;

        return switch (sortBy) {
            case CREATED_AT -> isDescending ? c.createdAt.desc() : c.createdAt.asc();
        };
    }
}

