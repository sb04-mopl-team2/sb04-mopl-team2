package com.codeit.mopl.domain.user.repository;

import com.codeit.mopl.domain.user.dto.request.CursorRequestUserDto;
import com.codeit.mopl.domain.user.dto.response.UserDto;
import com.codeit.mopl.domain.user.entity.Role;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.codeit.mopl.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements CustomUserRepository{
    private final JPAQueryFactory jpaQueryFactory;

    public Long countTotalElements(String emailLike) {
        String emailLikeValue = "";
        if (StringUtils.hasText(emailLike)) {
            emailLikeValue = emailLike;
        }

        Long totalElements = jpaQueryFactory.select(user.count())
                .from(user)
                .where(partialMatch(emailLikeValue))
                .fetchFirst();

        return totalElements;
    }

    public Slice findAllPage(CursorRequestUserDto request) {
        String emailLike = "";
        if (StringUtils.hasText(request.emailLike())) {
            emailLike = request.emailLike();
        }
        OrderSpecifier[] orderSpecifiers = createOrderSpecifier(request.sortBy(), request.sortDirection());

        JPAQuery query = jpaQueryFactory
                .select(
                        Projections.constructor(
                                UserDto.class,
                                user.id.as("id"),
                                user.createdAt.as("createdAt"),
                                user.email.as("email"),
                                user.name.as("name"),
                                user.profileImageUrl.as("profileImageUrl"),
                                user.role.as("role"),
                                user.locked.as("locked")
                        )
                )
                .from(user)
                .where(partialMatch(emailLike))
                .orderBy(orderSpecifiers)
                .limit(request.limit()+1);

        if (StringUtils.hasText(request.cursor())) {
            query.where(createCursorAfter(request.sortBy(), request.sortDirection(), request.cursor(), request.idAfter()));
        }

        if (request.roleEqual() != null) {
            query.where(user.role.eq(request.roleEqual()));
        }
        if(request.isLocked() != null) {
            query.where(user.locked.eq(request.isLocked()));
        }

        List<UserDto> content = query.fetch();

        boolean hasNext = content!=null && content.size() > request.limit();
        if (hasNext) {
            content = content.subList(0, request.limit());
        }
        Sort.Direction sortDirection = request.sortDirection().equals("ASCENDING") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return new SliceImpl<>(content, PageRequest.of(0,request.limit(), sortDirection,request.sortBy()),hasNext);
    }

    private OrderSpecifier[] createOrderSpecifier(String orderBy, String direction) {
        List<OrderSpecifier> orderSpecifier = new ArrayList<>();
        Order directionOrder = direction.equalsIgnoreCase("ASCENDING") ? Order.ASC : Order.DESC;

        switch (orderBy) {
            case "name" -> orderSpecifier.add(new OrderSpecifier(directionOrder, user.name));
            case "email" -> orderSpecifier.add(new OrderSpecifier(directionOrder, user.email));
            case "createdAt" -> orderSpecifier.add(new OrderSpecifier(directionOrder, user.createdAt));
            case "isLocked" -> orderSpecifier.add(new OrderSpecifier(directionOrder, user.locked));
            case "role" -> orderSpecifier.add(new OrderSpecifier(directionOrder, user.role));
        }

        orderSpecifier.add(new OrderSpecifier(directionOrder, user.id));
        return orderSpecifier.toArray(new OrderSpecifier[orderSpecifier.size()]);
    }

    private BooleanBuilder createCursorAfter(String orderBy, String direction, String cursor, UUID after) {
        BooleanBuilder predicate = new BooleanBuilder();
        Order directionOrder = direction.equalsIgnoreCase("ASCENDING") ? Order.ASC : Order.DESC;

        if (orderBy.equalsIgnoreCase("name")) {
            if (directionOrder == Order.ASC) {
                predicate.and(
                        user.name.gt(cursor)
                                .or(after != null
                                        ? user.name.eq(cursor).and(user.id.gt(after))
                                        : user.name.gt(cursor))
                );
            } else {
                predicate.and(
                        user.name.lt(cursor)
                                .or(after != null
                                        ? user.name.eq(cursor).and(user.id.lt(after))
                                        : user.name.lt(cursor))
                );
            }
        } else if (orderBy.equalsIgnoreCase("email")) {
            if (directionOrder == Order.ASC) {
                predicate.and(
                        user.email.gt(cursor)
                                .or(after != null
                                        ? user.email.eq(cursor).and(user.id.gt(after))
                                        : user.email.gt(cursor))
                );
            } else {
                predicate.and(
                        user.email.lt(cursor)
                                .or(after != null
                                        ? user.email.eq(cursor).and(user.id.lt(after))
                                        : user.email.lt(cursor))
                );
            }
        } else if (orderBy.equalsIgnoreCase("createdAt")) {
            LocalDateTime cursorValue = LocalDateTime.parse(String.valueOf(cursor));
            if (directionOrder == Order.ASC) {
                predicate.and(
                        user.createdAt.gt(cursorValue)
                                .or(after != null
                                        ? user.createdAt.eq(cursorValue).and(user.id.gt(after))
                                        : user.createdAt.gt(cursorValue))
                );
            } else {
                predicate.and(
                        user.createdAt.lt(cursorValue)
                                .or(after != null
                                        ? user.createdAt.eq(cursorValue).and(user.id.lt(after))
                                        : user.createdAt.lt(cursorValue))
                );
            }
        } else if (orderBy.equalsIgnoreCase("isLocked")) {
            Boolean isLocked = Boolean.parseBoolean(String.valueOf(cursor));
            if (directionOrder == Order.ASC) {
                if (!isLocked) {
                    predicate.and(
                            (after != null
                                    ? user.locked.isFalse().and(user.id.gt(after))
                                    : user.locked.isFalse())
                                    .or(user.locked.isTrue())
                    );
                } else {
                    predicate.and(
                            after != null
                                    ? user.locked.isTrue().and(user.id.gt(after))
                                    : user.locked.isTrue()
                    );
                }
            } else {
                if (isLocked) {
                    predicate.and(
                            (after != null
                                    ? user.locked.isTrue().and(user.id.lt(after))
                                    : user.locked.isTrue())
                                    .or(user.locked.isFalse())
                    );
                } else {
                    predicate.and(
                            after != null
                            ? user.locked.isFalse().and(user.id.lt(after))
                                    :user.locked.isFalse()
                    );
                }
            }
        } else if (orderBy.equalsIgnoreCase("role")) {
            if (directionOrder == Order.ASC) {
                if (cursor.equals(String.valueOf(Role.ADMIN))) {
                    predicate.and(
                            (after != null
                            ? user.role.eq(Role.ADMIN).and(user.id.gt(after))
                                    : user.role.eq(Role.ADMIN))
                                    .or(user.role.eq(Role.USER))
                    );
                } else {
                    predicate.and(
                            after != null
                            ? user.role.eq(Role.USER).and(user.id.gt(after))
                                    :user.role.eq(Role.USER)
                    );
                }
            } else {
                if (cursor.equals(String.valueOf(Role.USER))) {
                    predicate.and(
                            (after != null
                                    ? user.role.eq(Role.USER).and(user.id.lt(after))
                                    : user.role.eq(Role.USER))
                                    .or(user.role.eq(Role.ADMIN))
                    );
                } else {
                    predicate.and(
                            after != null
                                    ? user.role.eq(Role.ADMIN).and(user.id.lt(after))
                                    :user.role.eq(Role.ADMIN)
                    );
                }
            }
        }

        return predicate;
    }

    // 부분일치 검색
    private BooleanBuilder partialMatch(String emailLike) {
        BooleanBuilder predicate = new BooleanBuilder();

        predicate.or(user.email.containsIgnoreCase(emailLike));

        return predicate;
    }
}
