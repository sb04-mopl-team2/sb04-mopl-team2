package com.codeit.mopl.security.jwt;

import com.codeit.mopl.domain.user.dto.response.UserDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtInformation {
    private UserDto userDto;
    private String accessToken;
    private String refreshToken;

    public void rotate(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
