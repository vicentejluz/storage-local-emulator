package com.vicente.storage.validation;

import com.vicente.storage.dto.UploadMetadataDTO;
import com.vicente.storage.exception.InvalidUploadMetadataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;

public final class HeaderValidator {
    private static final Logger logger = LoggerFactory.getLogger(HeaderValidator.class);

    private HeaderValidator(){}

    public static void validateUploadHeaders(UploadMetadataDTO metadata){
        if(metadata.contentLength() == 0) {
            throw new InvalidUploadMetadataException("Content-Length must be greater than zero");
        }

        validateChecksum(metadata);

        validateContentDisposition(metadata);
    }

    private static void validateChecksum(UploadMetadataDTO metadata) {
        if (metadata.checksum() != null) {
            String checksum = metadata.checksum().trim();

            if(checksum.isEmpty()) {
                throw new InvalidUploadMetadataException("X-Content-MD5 cannot be empty if provided");
            }

            if(!ValidationPatterns.MD5_HEX_PATTERN.matcher(checksum).matches()) {
                throw new InvalidUploadMetadataException("X-Content-MD5 must be a valid MD5 hex string");
            }
        }
    }

    private static void validateContentDisposition(UploadMetadataDTO metadata) {
        if (metadata.contentDisposition() != null){
            String contentDisposition = metadata.contentDisposition().trim();

            if(contentDisposition.isEmpty()) {
                throw new InvalidUploadMetadataException("Content-Disposition cannot be empty if provided");
            }

            try {
                // O Spring tenta decompor o header. Se o formato for lixo, ele lança IllegalArgumentException.
                ContentDisposition cd = ContentDisposition.parse(contentDisposition);

                // Validação extra: Geralmente exige que seja inline ou attachment
                if(!cd.isAttachment() && !cd.isInline()){
                    throw new InvalidUploadMetadataException("Content-Disposition type must be inline or attachment");
                }

                if(cd.getFilename() != null){
                    if (cd.getFilename().contains("/") || cd.getFilename().contains("\\")) {
                        throw new InvalidUploadMetadataException("Content-Disposition filename contains invalid characters");
                    }
                }

            }catch (IllegalArgumentException e) {
                logger.debug("Invalid Content-Disposition header: {}", contentDisposition, e);
                throw new InvalidUploadMetadataException("Invalid Content-Disposition header format");
            }
        }
    }
}
