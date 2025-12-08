package com.codeit.mopl.oauth.userInfo;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getProviderEmail();
    String getProviderName();
    String getProfileImageUrl();
}
