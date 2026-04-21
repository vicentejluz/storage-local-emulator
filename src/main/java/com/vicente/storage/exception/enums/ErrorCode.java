package com.vicente.storage.exception.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_STORED_FILE_METADATA_ERROR("InvalidStoredFileMetadataError", HttpStatus.BAD_REQUEST),
    INVALID_UPLOAD_METADATA_ERROR("InvalidUploadMetadataError", HttpStatus.BAD_REQUEST),
    INCOMPLETE_UPLOAD_ERROR("IncompleteUploadError", HttpStatus.BAD_REQUEST),
    INVALID_STORAGE_PATH_ERROR("InvalidStoragePathError", HttpStatus.BAD_REQUEST),
    HASH_ALGORITHM_ERROR("HashAlgorithmError", HttpStatus.BAD_REQUEST),
    CHECKSUM_MISMATCH_ERROR("ChecksumMismatchError", HttpStatus.UNPROCESSABLE_CONTENT),
    BUCKET_CREATION_ERROR("BucketCreationError", HttpStatus.INTERNAL_SERVER_ERROR),
    METADATA_PERSISTENCE_ERROR("MetadataPersistenceError", HttpStatus.INTERNAL_SERVER_ERROR),
    MASTER_KEY_STORAGE_ERROR("MasterKeyStorageError", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_CLEANUP_ERROR("FileCleanupError", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_STORAGE_ERROR("FileStorageError", HttpStatus.INTERNAL_SERVER_ERROR),
    CHECKSUM_MOVE_ERROR("ChecksumMoveError", HttpStatus.INTERNAL_SERVER_ERROR),
    ACTIVE_MASTER_KEY_NOT_FOUND_ERROR("ActiveMasterKeyNotFoundError", HttpStatus.INTERNAL_SERVER_ERROR),
    SECRET_KEY_CRYPTO_ERROR("SecretKeyCryptoError", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ROOT_SECRET_KEY_ERROR("InvalidRootSecretKeyError", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("InternalError", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String value;
    private final HttpStatus status;

    ErrorCode(String value, HttpStatus status) {
        this.value = value;
        this.status = status;
    }

    public String value() {
        return value;
    }

    public HttpStatus status() {
        return status;
    }

}
