package com.codeit.mopl.domain.playlist.dto;

import com.codeit.mopl.domain.notification.entity.SortDirection;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;

import java.util.UUID;

@Setter
public class PlaylistSearchCond {


        private String keywordLike;
        private UUID ownerIdEqual;
        private UUID subscriberIdEqual;

        private String cursor;
        private UUID idAfter;

        @NotNull(message = "limit 값은 필수입니다.")
        private Integer limit;

        @NotNull(message = "정렬 방향(sortDirection)은 필수입니다.")
        private SortDirection sortDirection;

        @NotNull(message = "정렬 기준(sortBy)은 필수입니다.")
        private SortBy sortBy;

        public enum SortBy {
                updatedAt,
                subscribeCount
        }
}
