package com.vicente.storage.support.file;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class FileTypeResolver {
    private final Tika tika = new Tika();
    Logger logger = LoggerFactory.getLogger(FileTypeResolver.class);

    public String detectContentType(Path physicalPathTemp, String virtualFileName) {
        Metadata metadata = new Metadata();

        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, virtualFileName);

        try (TikaInputStream is = TikaInputStream.get(physicalPathTemp)) {
            String mimeType = tika.detect(is, metadata);

            return Optional.ofNullable(mimeType).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        } catch (IOException e) {
            logger.error("Failed to detect content type for {}", virtualFileName, e);
            // Retorno padrão caso o arquivo esteja ilegível
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    public String resolveEffectiveContentType(String detectedType, String clientType){
        if(!MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(detectedType)){
            return detectedType;
        }

        return isValidMediaType(clientType) ? clientType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    public String detectExtension(String finalType, String virtualFileName){
        String extension = null;
        try{
            MimeType type = MimeTypes.getDefaultMimeTypes().forName(finalType);

            String ext = type.getExtension();
            if (StringUtils.hasText(ext)) {
                extension = ext.replaceFirst("^\\.", "");
            }
        } catch (MimeTypeException e) {
            logger.debug("Tika could not determine extension for type: {}", finalType);
        }

        // Se o Tika não encontrou, tenta pelo nome do arquivo
        if (!StringUtils.hasText(extension)) {
            extension = FilenameUtils.getExtension(virtualFileName);
        }

        // O Spring tem esse método utilitário que converte "" ou " " para null automaticamente
        return StringUtils.hasText(extension) ? extension : null;
    }

    private boolean isValidMediaType(String contentType){
        if (!StringUtils.hasText(contentType)) {
            return false;
        }

        try {
            MediaType.parseMediaType(contentType);
            return true;
        }catch (InvalidMediaTypeException e) {
            return false;
        }
    }
}
