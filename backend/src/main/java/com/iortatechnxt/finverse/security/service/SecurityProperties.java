package com.iortatechnxt.finverse.security.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Security settings bound from {@code finverse.security.*}.
 *
 * @param jwtSecret HMAC secret (min 32 bytes); must come from the environment in production
 * @param tokenValidity access token lifetime
 * @param allowedOrigins CORS origins permitted to call the API
 */
@Validated
@ConfigurationProperties(prefix = "finverse.security")
public record SecurityProperties(
    @NotBlank @Size(min = 32) String jwtSecret,
    @NotNull Duration tokenValidity,
    List<String> allowedOrigins) {}
