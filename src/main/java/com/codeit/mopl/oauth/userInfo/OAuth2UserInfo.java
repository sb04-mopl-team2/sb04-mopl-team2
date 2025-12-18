package com.codeit.mopl.oauth.userInfo;

import com.codeit.mopl.domain.user.entity.Provider;

public interface OAuth2UserInfo {
    String getProviderId();
    Provider getProvider();
    String getProviderEmail();
    String getProviderName();
    String getProfileImageUrl();
}
