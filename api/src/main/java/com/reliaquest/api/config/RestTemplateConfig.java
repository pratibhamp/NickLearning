package com.reliaquest.api.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for our HTTP client (RestTemplate).
 *
 * I've configured reasonable timeouts here - 10 seconds to establish a connection
 * and 30 seconds to read the response. The mock server can be a bit slow sometimes,
 * so I didn't want to make the timeouts too aggressive.
 *
 * In a production system, I'd probably add retry logic and circuit breakers,
 * but this should be good enough for our current needs.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
