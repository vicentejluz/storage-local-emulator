package com.vicente.storage.util;

import com.vicente.storage.exception.InvalidVirtualFileNameException;

public final class ObjectKeyPathParser {
    private ObjectKeyPathParser() {}

    public static String extractVirtualFileName(String objectKey) {
        int lastSlashIndex = objectKey.lastIndexOf('/');

        String virtualFileName = (lastSlashIndex == -1) ? objectKey : objectKey.substring(lastSlashIndex + 1);

        if (virtualFileName.isBlank()) {
            throw new InvalidVirtualFileNameException("Object key filename cannot be empty");
        }

        return virtualFileName;
    }

    public static String extractVirtualPath(String objectKey) {
        int lastSlashIndex = objectKey.lastIndexOf('/');

        // Se não tem barra, está na raiz (String vazia)
        // Se tem barra, pega do início até a última barra (incluindo ela ou não, depende do seu uso)
        if (lastSlashIndex == -1) return "";

        // Retorna o caminho com a barra final (ex: "fotos/viagem/")
        // Se preferir sem a barra final, use: substring(0, lastSlashIndex)
        return objectKey.substring(0, lastSlashIndex + 1);
    }
}
