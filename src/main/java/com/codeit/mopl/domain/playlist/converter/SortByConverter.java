package com.codeit.mopl.domain.playlist.converter;

import com.codeit.mopl.domain.playlist.entity.SortBy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("playlistSortByConverter")
public class SortByConverter implements Converter<String, SortBy> {
    @Override
    public SortBy convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("SortBy 값은 null이거나 빈 값일 수 없습니다");
        }

        for (SortBy v : SortBy.values()) {
            if (v.getValue().equalsIgnoreCase(source)) {
                return v;
            }
        }
        try {
            return SortBy.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "유효하지 않은 SortBy 값입니다: " + source, e);
        }
    }
}
