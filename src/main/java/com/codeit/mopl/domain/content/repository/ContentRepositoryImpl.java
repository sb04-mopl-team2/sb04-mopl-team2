package com.codeit.mopl.domain.content.repository;

import static com.codeit.mopl.domain.content.entity.QContent.content;

import com.codeit.mopl.domain.base.TimeUtil;
import com.codeit.mopl.domain.content.dto.request.ContentSearchCondition;
import com.codeit.mopl.domain.content.dto.response.ContentDto;
import com.codeit.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.content.entity.ContentType;
import com.codeit.mopl.domain.content.entity.SortBy;
import com.codeit.mopl.domain.content.entity.SortDirection;
import com.codeit.mopl.domain.content.mapper.ContentMapper;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentRepositoryImpl implements ContentRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final ContentMapper mapper;

  @Override
  public CursorResponseContentDto findContents(ContentSearchCondition cond) {
    if (cond.getSortBy() == null || cond.getSortDirection() == null) {
      throw new IllegalArgumentException("sortBy, sortDirection은 필수입니다.");
    }

    int limit = cond.getLimit();
    if (limit <= 0) {
      throw new IllegalArgumentException("limit는 1 이상이어야 합니다.");
    }

    // 1. 데이터 (limit + 1)
    List<Content> contents = queryFactory
        .selectFrom(content)
        .where(
            typeEqual(cond.getTypeEqual()),
            keywordLike(cond.getKeywordLike()),
            tagsIn(cond.getTagsIn()),
            cursorCondition(cond)
        )
        .orderBy(orderSpecifiers(cond))
        .limit(limit + 1)
        .fetch();

    boolean hasNext = contents.size() > limit;
    if (hasNext) {
      contents.remove(contents.size() - 1);
    }

    // nextCursor, nextIdAfter 계산
    String nextCursor = null;
    UUID nextIdAfter = null;

    if (!contents.isEmpty()) {
      Content last = contents.get(contents.size() - 1);

      nextCursor = switch (cond.getSortBy()) {
        case CREATED_AT -> last.getCreatedAt().toString();
        case WATCHER_COUNT -> String.valueOf(last.getWatcherCount());
        case RATE -> String.valueOf(last.getAverageRating());
      };

      nextIdAfter = last.getId();
    }

    // totalCount
    Long totalCount = queryFactory
        .select(content.count())
        .from(content)
        .where(
            typeEqual(cond.getTypeEqual()),
            keywordLike(cond.getKeywordLike()),
            tagsIn(cond.getTagsIn())
        )
        .fetchOne();

    List<ContentDto> data = contents.stream()
        .map(mapper::toDto)
        .toList();

    return new CursorResponseContentDto(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        cond.getSortBy().name(),
        cond.getSortDirection().name()
    );
  }

  // ---------- where 조건 ----------
  private BooleanExpression typeEqual(String typeEqual) {
    if (typeEqual == null) {
      return null;
    }
    return content.contentType.eq(ContentType.fromType(typeEqual));
  }

  private BooleanExpression keywordLike(String keyword) {
    if (keyword == null) {
      return null;
    }
    return content.title.containsIgnoreCase(keyword)
        .or(content.description.containsIgnoreCase(keyword));
  }

  private BooleanExpression tagsIn(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return null;
    }
    return content.tags.any().in(tags);
  }

  // ---------- 커서 조건 ----------
  private BooleanExpression cursorCondition(ContentSearchCondition cond) {
    if (cond.getCursor() == null) {
      return null;
    }

    SortBy sortBy = cond.getSortBy();
    SortDirection dir = cond.getSortDirection();
    UUID idAfter = cond.getIdAfter();

    switch (sortBy) {
      case CREATED_AT -> {
        LocalDateTime cursorLocalDateTime = LocalDateTime.parse(cond.getCursor());
        Instant cursorInstant = TimeUtil.toInstant(cursorLocalDateTime);

        if (dir == SortDirection.ASCENDING) {
          if (idAfter != null) {
            return content.createdAt.gt(cursorInstant)
                .or(content.createdAt.eq(cursorInstant).and(content.id.gt(idAfter)));
          } else {
            return content.createdAt.gt(cursorInstant);
          }
        } else { // DESCENDING
          if (idAfter != null) {
            return content.createdAt.lt(cursorInstant)
                .or(content.createdAt.eq(cursorInstant).and(content.id.gt(idAfter)));
          } else {
            return content.createdAt.lt(cursorInstant);
          }
        }
      }

      case WATCHER_COUNT -> {
        Integer cursorInt = (Integer) parseCursor(sortBy, cond.getCursor());
        if (dir == SortDirection.ASCENDING) {
          if (idAfter != null) {
            return content.watcherCount.gt(cursorInt)
                .or(content.watcherCount.eq(cursorInt).and(content.id.gt(idAfter)));
          } else {
            return content.watcherCount.gt(cursorInt);
          }
        } else {
          if (idAfter != null) {
            return content.watcherCount.lt(cursorInt)
                .or(content.watcherCount.eq(cursorInt).and(content.id.gt(idAfter)));
          } else {
            return content.watcherCount.lt(cursorInt);
          }
        }
      }

      case RATE -> {
        Double cursorDouble = (Double) parseCursor(sortBy, cond.getCursor());
        if (dir == SortDirection.ASCENDING) {
          if (idAfter != null) {
            return content.averageRating.gt(cursorDouble)
                .or(content.averageRating.eq(cursorDouble).and(content.id.gt(idAfter)));
          } else {
            return content.averageRating.gt(cursorDouble);
          }
        } else {
          if (idAfter != null) {
            return content.averageRating.lt(cursorDouble)
                .or(content.averageRating.eq(cursorDouble).and(content.id.gt(idAfter)));
          } else {
            return content.averageRating.lt(cursorDouble);
          }
        }
      }

      default -> throw new IllegalArgumentException("unsupported sortBy: " + sortBy);
    }
  }

  private Comparable<?> parseCursor(SortBy sortBy, String cursor) {
    return switch (sortBy) {
      case CREATED_AT -> LocalDateTime.parse(cursor);
      case WATCHER_COUNT -> Integer.valueOf(cursor);
      case RATE -> Double.valueOf(cursor);
    };
  }

  // ---------- ORDER BY ----------
  private OrderSpecifier<?>[] orderSpecifiers(ContentSearchCondition cond) {
    Order direction = cond.getSortDirection() == SortDirection.ASCENDING ?
        Order.ASC : Order.DESC;

    return switch (cond.getSortBy()) {
      case CREATED_AT -> new OrderSpecifier[]{
          new OrderSpecifier<>(direction, content.createdAt),
          new OrderSpecifier<>(Order.ASC, content.id)
      };
      case WATCHER_COUNT -> new OrderSpecifier[]{
          new OrderSpecifier<>(direction, content.watcherCount),
          new OrderSpecifier<>(Order.ASC, content.id)
      };
      case RATE -> new OrderSpecifier[]{
          new OrderSpecifier<>(direction, content.averageRating),
          new OrderSpecifier<>(Order.ASC, content.id)
      };
    };
  }
}


