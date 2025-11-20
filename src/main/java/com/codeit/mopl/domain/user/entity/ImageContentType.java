package com.codeit.mopl.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageContentType {
    JPG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp"),
    SVG("image/svg+xml");

    private final String mimeType;

    public static boolean isImage(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        for (ImageContentType type : values()) {
            if (type.getMimeType().equalsIgnoreCase(mimeType)) {
                return true;
            }
        }
        return false;
    }
}
