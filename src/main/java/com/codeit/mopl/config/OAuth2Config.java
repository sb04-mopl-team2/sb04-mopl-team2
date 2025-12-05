package com.codeit.mopl.config;

import com.codeit.mopl.oauth.client.GoogleClientProperties;
import com.codeit.mopl.oauth.client.KakaoClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GoogleClientProperties.class, KakaoClientProperties.class})
public class OAuth2Config {
}
