package com.vicente.storage.service.impl;

import com.vicente.storage.domain.StoredFileMetadata;
import com.vicente.storage.domain.vo.PhysicalFileNames;
import com.vicente.storage.dto.UploadMetadataDTO;
import com.vicente.storage.exception.*;
import com.vicente.storage.repository.StoredFileMetadataRepository;
import com.vicente.storage.support.cleanup.FileCleanupHandler;
import com.vicente.storage.support.retry.StorageRetryExecutor;
import com.vicente.storage.service.LocalStorageEmulatorService;
import com.vicente.storage.util.FileSuffixes;
import com.vicente.storage.util.PathValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocalStorageEmulatorServiceImpl implements LocalStorageEmulatorService {
    private final Path rootPath;
    private final StoredFileMetadataRepository storedFileMetadataRepository;
    private final StorageRetryExecutor storageRetryExecutor;
    private final FileCleanupHandler fileCleanupHandler;
    private static final String HASH_ALGORITHM = "MD5";
    private static final  Logger logger = LoggerFactory.getLogger(LocalStorageEmulatorServiceImpl.class);

    public LocalStorageEmulatorServiceImpl(
            @Value("${storage.emulator.root.path}") String rootPath,
            StoredFileMetadataRepository storedFileMetadataRepository,
            StorageRetryExecutor storageRetryExecutor, FileCleanupHandler fileCleanupHandler
    ) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
        this.storedFileMetadataRepository = storedFileMetadataRepository;
        this.storageRetryExecutor = storageRetryExecutor;
        this.fileCleanupHandler = fileCleanupHandler;
    }

    @Override
    @Transactional
    public String saveFile(InputStream fileStream, UploadMetadataDTO metadata) {
        PhysicalFileNames physicalFileNames = generatePhysicalFileNames();

        String physicalFileName = physicalFileNames.physicalFileName();
        String physicalFileNameTemp = physicalFileNames.physicalFileNameTemp();

        Path filesPath = resolveChildPath(rootPath, "storage");
        Path bucketPath = resolveChildPath(filesPath, metadata.bucket());
        Path physicalPath = bucketPath.resolve(physicalFileName).normalize();
        Path checksumPath = bucketPath.resolve(physicalFileName + FileSuffixes.CHECKSUM_SUFFIX).normalize();
        Path physicalPathTemp = bucketPath.resolve(physicalFileNameTemp).normalize();
        Path checksumTmpPath = bucketPath.resolve(
                checksumPath.getFileName().toString() + FileSuffixes.TEMP_FILE_SUFFIX).normalize();

        ensureBucketDirectoryAndValidatePaths(bucketPath, physicalPath, physicalPathTemp, checksumTmpPath, checksumPath);

        StoredFileMetadata storedFileMetadata = new StoredFileMetadata(metadata.objectKey(), "teste",
                physicalFileName, ".teste", metadata.contentType(),
                metadata.contentDisposition(), "teste/teste", 1L);

        String etag = processTempFileAndChecksum(fileStream, metadata, storedFileMetadata, physicalPathTemp, checksumTmpPath);

        validateChecksumIfPresent(metadata, etag, physicalPathTemp, checksumTmpPath);

        Optional<StoredFileMetadata> optionalStoredFileMetadata = storedFileMetadataRepository
                .findByBucketIdAndObjectKey(1L, metadata.objectKey());

        String oldPhysicalFileName = null;
        if(optionalStoredFileMetadata.isPresent()){
            StoredFileMetadata oldStoredFileMetadata = optionalStoredFileMetadata.get();
            if(etag.equals(oldStoredFileMetadata.getEtag())){
                return skipUploadIfSameEtag(metadata, physicalPathTemp, checksumTmpPath, etag);
            }
            oldPhysicalFileName = oldStoredFileMetadata.getPhysicalFileName();
        }

        storedFileMetadata.setEtag(etag);

        storageRetryExecutor.saveMetadataWithRetry(storedFileMetadata, physicalPathTemp, checksumTmpPath);

        storageRetryExecutor.renameRetry(physicalPathTemp, physicalPath, checksumTmpPath, checksumPath);

        scheduleCleanupAfterCommit(oldPhysicalFileName, bucketPath);

        return etag;
    }

    private PhysicalFileNames generatePhysicalFileNames() {
        String uuid = UUID.randomUUID().toString();
        String physicalFileName = uuid + FileSuffixes.PHYSICAL_FILE_SUFFIX;
        String physicalFileNameTemp = uuid + FileSuffixes.TEMP_FILE_SUFFIX;
        return new PhysicalFileNames(physicalFileName, physicalFileNameTemp);
    }

    private void ensureBucketDirectoryAndValidatePaths(Path bucketPath, Path physicalPath,
                                                       Path physicalPathTemp, Path checksumTmpPath, Path checksumPath) {

        PathValidator.ensurePathsWithinRoot(bucketPath, physicalPath, physicalPathTemp, checksumTmpPath, checksumPath);
        try {
            if(Files.notExists(bucketPath)) {
                logger.info("Creating new bucket directory: {}", bucketPath);
                Files.createDirectories(bucketPath);
            }
        }catch (IOException e){
            logger.error("Failed to ensure bucket directory exists: {}", bucketPath, e);
            throw new BucketCreationException("Could not initialize bucket storage: " + bucketPath.getFileName());
        }
    }

    private String processTempFileAndChecksum(InputStream fileStream, UploadMetadataDTO metadata,
                                              StoredFileMetadata storedFileMetadata, Path physicalPathTemp,
                                              Path checksumTmpPath) {
        try{
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);

            long bytesCount = copyStreamAndCountBytes(fileStream, md, physicalPathTemp);

            validateContentLength(metadata, bytesCount, physicalPathTemp);

            storedFileMetadata.setContentLength(bytesCount);

            String etag = HexFormat.of().formatHex(md.digest());
            Files.writeString(checksumTmpPath, etag,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return etag;
        } catch (IOException e) {
            logger.warn("Cleaning up temporary files due to IO error: {}, {}",
                    physicalPathTemp, checksumTmpPath);
            fileCleanupHandler.deleteFilesIfExists(physicalPathTemp, checksumTmpPath);
            logger.error("IO error during file processing");
            throw new FileStorageException("Error occurred while reading/writing file stream");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to initialize {} digest", HASH_ALGORITHM);
            throw new HashAlgorithmException(HASH_ALGORITHM +" algorithm not available");
        }

    }

    private void validateContentLength(UploadMetadataDTO metadata, long bytesCount, Path physicalPathTemp) throws IOException {
        if (metadata.contentLength() > 0 && metadata.contentLength() != bytesCount) {
            Files.deleteIfExists(physicalPathTemp);
            logger.warn("Uploaded bytes ({}) do not match the expected content length ({}) for file {}",
                    bytesCount, metadata.contentLength(), physicalPathTemp);
            throw new IncompleteUploadException("Upload incomplete: expected " + metadata.contentLength() +
                    " bytes but received " + bytesCount + " bytes");
        }
    }

    private long copyStreamAndCountBytes(InputStream fileStream, MessageDigest md, Path physicalPathTemp) throws IOException {
        long bytesCount = 0;
        try (DigestInputStream dis = new DigestInputStream(fileStream, md);
             OutputStream out = Files.newOutputStream(physicalPathTemp)) {
            byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];

            int read;
            while ((read = dis.read(buffer)) != -1) {
                out.write(buffer, 0, read);

                bytesCount += read;
            }
        }
        return bytesCount;
    }

    private void validateChecksumIfPresent(UploadMetadataDTO metadata, String etag, Path physicalPathTemp, Path checksumTmpPath) {
        if(metadata.checksum() != null && !metadata.checksum().isBlank()) {
            if(!etag.equalsIgnoreCase(metadata.checksum())) {
                logger.warn("Checksum mismatch for objectKey={}, expected={}, actual={}",
                        metadata.objectKey(), metadata.checksum(), etag);
                fileCleanupHandler.deleteFilesIfExists(physicalPathTemp, checksumTmpPath);
                throw new ChecksumMismatchException("Uploaded file checksum does not match server calculated checksum");
            }
        }
    }

    private String skipUploadIfSameEtag(UploadMetadataDTO metadata, Path physicalPathTemp, Path checksumTmpPath, String etag) {
        logger.info("Skipping upload. Same ETag for objectKey={}", metadata.objectKey());
        fileCleanupHandler.deleteFilesIfExists(physicalPathTemp, checksumTmpPath);
        return etag;
    }

    private void scheduleCleanupAfterCommit(String oldPhysicalFileName, Path bucketPath) {
        if (oldPhysicalFileName != null && !oldPhysicalFileName.isBlank()){
            Path oldPhysicalPath = bucketPath.resolve(oldPhysicalFileName).normalize();
            Path oldChecksumPath = bucketPath.resolve(oldPhysicalFileName + FileSuffixes.CHECKSUM_SUFFIX).normalize();
            try {
                PathValidator.ensurePathsWithinRoot(bucketPath,oldPhysicalPath, oldChecksumPath);
            }catch (InvalidStoragePathException e) {
                logger.warn("Skipping cleanup due to invalid path for file: {}", oldPhysicalFileName, e);
                return;
            }
            logger.debug("Async cleanup scheduled for files: {}", oldPhysicalFileName);
            // Registra uma ação para ser executada em sincronia com o ciclo de vida da transação atual.
            // Esse método "liga" seu código ao commit/rollback da transação do Spring.
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronization(
                    oldPhysicalFileName, oldPhysicalPath, oldChecksumPath));
        }
    }

    private TransactionSynchronization transactionSynchronization(
            String oldPhysicalFileName, Path oldPhysicalPath, Path oldChecksumPath) {
        // Cria uma implementação anônima da interface TransactionSynchronization.
        // Essa interface permite reagir a eventos da transação (beforeCommit, afterCommit, afterRollback, etc).
        return new TransactionSynchronization() {
            // Esse método é chamado SOMENTE após o commit da transação ser concluído com sucesso.
            // Ou seja:
            // ✔ o banco já persistiu os dados definitivamente
            // ✔ não haverá rollback depois disso
            // ❌ NÃO é chamado se a transação falhar
            @Override
            public void afterCommit() {
                logger.debug("Transaction committed. Triggering async cleanup for file: {}", oldPhysicalFileName);
                // Dispara o cleanup de forma assíncrona (@Async).
                // Isso garante que:
                // ✔ não bloqueia a thread da requisição principal
                // ✔ o cleanup roda em background
                // ✔ só executa após o banco estar consistente
                fileCleanupHandler.triggerAsyncCleanup(oldPhysicalFileName, oldPhysicalPath, oldChecksumPath);
            }
        };
    }

    private Path resolveChildPath(Path parent, String child) {
        Path resolved = parent.resolve(child).normalize();
        PathValidator.ensurePathsWithinRoot(parent, resolved);
        return resolved;
    }
}
