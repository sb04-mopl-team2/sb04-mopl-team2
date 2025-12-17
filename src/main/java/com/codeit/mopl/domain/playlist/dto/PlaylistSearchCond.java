package com.codeit.mopl.domain.playlist.dto;

import com.codeit.mopl.domain.base.SortDirection;
import com.codeit.mopl.domain.base.SortBy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class PlaylistSearchCond {

        private String keywordLike;
        private UUID ownerIdEqual;
        private UUID subscriberIdEqual;

        private String cursor;
        private UUID idAfter;

        @Positive
        @Max(100)
        @NotNull(message = "limit 값은 필수입니다.")
        private Integer limit;

        @NotNull(message = "정렬 방향(sortDirection)은 필수입니다.")
        private SortDirection sortDirection;

        @NotNull(message = "정렬 기준(sortBy)은 필수입니다.")
        private SortBy sortBy;

        public PlaylistSearchCond withoutCursor () {
                PlaylistSearchCond cond = new PlaylistSearchCond();
                cond.setKeywordLike(this.keywordLike);
                cond.setOwnerIdEqual(this.ownerIdEqual);
                cond.setSubscriberIdEqual(this.subscriberIdEqual);
                cond.setCursor(null);
                cond.setIdAfter(null);
                cond.setLimit(this.limit);
                cond.setSortDirection(this.sortDirection);
                cond.setSortBy(this.sortBy);
                return cond;
        }
}
