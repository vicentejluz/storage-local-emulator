package com.vicente.storage.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "storage.emulator.root")
@Validated
public record StorageProperties(
    @NotBlank
    String path,

    @NotBlank
    @Pattern(
            regexp = "^[A-Za-z0-9\\-_]{3,64}$",
            message = "Access key must be alphanumeric (including hyphens and underscores) and between 3 and 64 characters"
    )
    String accessKey,
    @NotBlank
    String secretKey,
    String initialBucket
) {
}
