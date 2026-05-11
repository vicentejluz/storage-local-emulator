package com.vicente.storage.util;

import org.springframework.beans.factory.annotation.Value;
import com.vicente.storage.validation.PathValidator;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class StoragePathResolver {
    private final Path rootPath;

    public StoragePathResolver(@Value("${storage.emulator.root.path}") String rootPath) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    public Path root() {
        return rootPath;
    }

    public Path storageDir(){
        return resolve(rootPath, "storage");
    }

    public Path keysDir(){
        return resolve(rootPath, "keys");
    }

    public Path masterKey(Long version) {
        return resolve(keysDir(), "master-key-" + version + FileSuffixes.KEY_SUFFIX);
    }

    public Path bucketDir(String bucket) {
        return resolve(storageDir(), bucket);
    }

    public Path physicalFile(String bucket, String fileName) {
        return resolve(bucketDir(bucket), fileName);

    }

    public Path checksumFile(String bucket, String fileName) {
        return resolve(bucketDir(bucket), fileName + FileSuffixes.CHECKSUM_SUFFIX);
    }

    public Path tempFile(String bucket, String fileName) {
        return resolve(bucketDir(bucket), fileName + FileSuffixes.TEMP_FILE_SUFFIX);
    }

    private Path resolve(Path parent, String child) {
        Path resolved = parent.resolve(child).normalize();
        PathValidator.ensurePathsWithinRoot(parent, resolved);
        return resolved;

    }
}
