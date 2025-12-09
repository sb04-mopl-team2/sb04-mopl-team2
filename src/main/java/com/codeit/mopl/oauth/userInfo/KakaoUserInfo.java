package com.codeit.mopl.oauth.userInfo;

import com.codeit.mopl.domain.user.entity.Provider;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class KakaoUserInfo implements OAuth2UserInfo{
    private Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public String getProviderEmail() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        return properties.get("nickname") + "_" + attributes.get("id") + "@kakao.com";
    }

    @Override
    public String getProviderName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        return (String) properties.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        return (String) properties.get("profile_image");
    }
}
