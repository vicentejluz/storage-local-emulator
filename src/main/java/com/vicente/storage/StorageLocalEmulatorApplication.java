package com.vicente.storage;

import com.vicente.storage.dao.StoredFileMetadataDAO;
import com.vicente.storage.domain.entity.StoredFileMetadata;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Optional;
import java.util.UUID;

@SpringBootApplication
public class StorageLocalEmulatorApplication implements CommandLineRunner {
	private final StoredFileMetadataDAO repository;
	private static final Logger log = LoggerFactory.getLogger(StorageLocalEmulatorApplication.class);

    public StorageLocalEmulatorApplication(StoredFileMetadataDAO repository) {
        this.repository = repository;
    }

    static void main(String[] args) {
		SpringApplication.run(StorageLocalEmulatorApplication.class, args);
	}


	@Override
	public void run(String @NonNull ... args) {
		log.info("==== START StoredFileMetadata TEST ====");

		// insert
		StoredFileMetadata metadata = new StoredFileMetadata(
				"documents/test.txt",
				"test.txt",
				UUID.randomUUID().toString(),
				"txt",
				"text/plain",
				100L,
				"/documents",
				"my-bucket",
				"abc123checksum"
		);

		repository.save(metadata);
		repository.save(metadata);

		Optional<StoredFileMetadata> found = repository.findByBucketAndObjectKey(metadata.bucket(), metadata.objectKey());

		if (found.isPresent()) {
			log.info("Found by id: {}", found.get().id());


			log.info("found={}", found.get());

			// exists
			boolean exists = repository.existsByBucketAndObjectKey(
					found.get().bucket(),
					found.get().objectKey()
			);

			log.info("Exists before delete: {}", exists);

			repository.deleteByBucketAndObjectKey(found.get().bucket(), found.get().objectKey());
			log.info("Deleted by id: {}", found.get().id());

			boolean existsAfter = repository.existsByBucketAndObjectKey(
					found.get().bucket(),
					found.get().objectKey()
			);
			log.info("Exists after delete: {}", existsAfter);
		}

		log.info("==== END StoredFileMetadata TEST ====");
	}
}


