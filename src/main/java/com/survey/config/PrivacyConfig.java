package com.survey.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrivacyConfig {

    private final boolean audienceCollectionEnabled;

    public PrivacyConfig(@Value("${app.privacy.audience-enabled:true}") boolean audienceCollectionEnabled) {
        this.audienceCollectionEnabled = audienceCollectionEnabled;
    }

    public boolean isAudienceCollectionEnabled() {
        return audienceCollectionEnabled;
    }
}
