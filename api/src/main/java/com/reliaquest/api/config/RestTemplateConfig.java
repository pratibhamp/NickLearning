package com.reliaquest.api.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * This class sets up our HTTP client (RestTemplate).
 * The timeouts are set to 10 seconds for connecting and 30 seconds for reading data.
 * The mock server can be slow, so these timeouts should be enough.
 * For real systems, we might add retries and circuit breakers, but this is fine for now.
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
