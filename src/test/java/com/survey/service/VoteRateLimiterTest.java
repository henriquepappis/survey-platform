package com.survey.service;

import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VoteRateLimiterTest {

    @Test
    @DisplayName("Rate limiter deve permitir até o limite e bloquear o próximo")
    void shouldBlockAfterLimit() {
        VoteRateLimiter limiter = new VoteRateLimiter(2, Clock.systemUTC());

        assertThat(limiter.allow("1.1.1.1")).isTrue();
        assertThat(limiter.allow("1.1.1.1")).isTrue();
        assertThat(limiter.allow("1.1.1.1")).isFalse();
    }
}
