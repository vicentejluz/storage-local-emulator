package com.vicente.storage.validation;

import com.vicente.storage.exception.InvalidObjectKeyException;

import java.text.Normalizer;

public final class ObjectKeyNormalizer {

    private ObjectKeyNormalizer() {}

    public static String normalize(String objectKey){
        // 1. Normalização Unicode (NFC) - Garante consistência de caracteres acentuados
        String normalizedObjectKey = Normalizer.normalize(objectKey, Normalizer.Form.NFC);

        // 2. Remove todas as barras iniciais de uma vez
        normalizedObjectKey = normalizedObjectKey.replaceFirst("^/+", "");

        // 3. Valida segmentos (Path Traversal)
        validatePathTraversal(normalizedObjectKey);

        // 4. Valida barras duplicadas (ex: pasta//arquivo)
        validateDuplicateSlashes(normalizedObjectKey);

        if (normalizedObjectKey.isBlank()) {
            throw new InvalidObjectKeyException("Object key cannot be empty");
        }

        return normalizedObjectKey;
    }

    private static void validatePathTraversal(String normalizedObjectKey) {
        String[] segments = normalizedObjectKey.split("/", -1);
        for(String segment : segments){
            if(segment.equals(".") || segment.equals("..")){
                throw new InvalidObjectKeyException("Object key contains invalid path traversal sequence");
            }
        }
    }

    private static void validateDuplicateSlashes(String normalizedObjectKey){
        if(normalizedObjectKey.contains("//")){
            throw new InvalidObjectKeyException("Object key contains duplicate slashes");
        }
    }
}
