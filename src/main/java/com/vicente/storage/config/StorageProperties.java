package com.vicente.storage.config;

import com.vicente.storage.validation.ValidationPatterns;
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
            regexp = ValidationPatterns.ACCESS_KEY_REGEX,
            message = "Access key must be alphanumeric (including hyphens and underscores) and between 3 and 64 characters"
    )
    String accessKey,
    @NotBlank
    String secretKey,
    @Pattern(
            regexp = ValidationPatterns.BUCKET_NAME_REGEX,
            message = "Bucket name must be 3-63 characters long, contain only lowercase letters, numbers, and hyphens, " +
                    "must not start or end with a hyphen, and cannot contain consecutive hyphens."
    )
    String initialBucket
) {
    public StorageProperties{
        initialBucket = normalize(initialBucket);
    }

    private static String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
