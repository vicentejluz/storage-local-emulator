package com.vicente.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StorageLocalEmulatorApplication {
    static void main(String[] args) {
		SpringApplication.run(StorageLocalEmulatorApplication.class, args);
	}
}


