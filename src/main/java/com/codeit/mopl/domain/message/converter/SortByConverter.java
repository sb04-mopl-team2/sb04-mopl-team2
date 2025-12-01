package com.codeit.mopl.domain.message.converter;

import com.codeit.mopl.domain.playlist.entity.SortBy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("messageSortByConverter")
public class SortByConverter implements Converter<String, SortBy> {
    @Override
    public SortBy convert(String source) {
        for (SortBy v : SortBy.values()) {
            if (v.getValue().equalsIgnoreCase(source)) {
                return v;
            }
        }
        return SortBy.valueOf(source.toUpperCase());
    }
}
