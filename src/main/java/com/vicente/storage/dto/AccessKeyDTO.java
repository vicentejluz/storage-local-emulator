package com.vicente.storage.dto;

public record AccessKeyDTO(
        String accessKey,
        String secretKey,
        Long masterKeyVersion,
        String status) {
}
