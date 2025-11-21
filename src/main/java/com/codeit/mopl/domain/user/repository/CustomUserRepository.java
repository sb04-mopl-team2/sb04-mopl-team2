package com.codeit.mopl.domain.user.repository;

import com.codeit.mopl.domain.user.dto.request.CursorRequestUserDto;
import org.springframework.data.domain.Slice;

public interface CustomUserRepository {
    Slice findAllPage(CursorRequestUserDto request);
    Long countTotalElements(String emailLike);
}
