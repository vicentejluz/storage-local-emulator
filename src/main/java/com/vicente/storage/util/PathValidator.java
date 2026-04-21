package com.vicente.storage.util;

import com.vicente.storage.exception.InvalidStoragePathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class PathValidator {

    private static final Logger logger = LoggerFactory.getLogger(PathValidator.class);

    private PathValidator() {
    }

    public static void ensurePathsWithinRoot(Path root, Path... paths) {
        for (Path path : paths) {
            if (!path.startsWith(root)) {
                logger.error("Attempt to access path outside allowed root | root={} | path={}", root, path);
                throw new InvalidStoragePathException("Invalid file path: access outside allowed root");
            }
        }
    }

}
