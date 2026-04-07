package com.vicente.storage.controller;

import com.vicente.storage.service.LocalStorageEmulatorService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class LocalStorageEmulatorController {
    private final LocalStorageEmulatorService localStorageEmulatorService;

    public LocalStorageEmulatorController(LocalStorageEmulatorService localStorageEmulatorService) {
        this.localStorageEmulatorService = localStorageEmulatorService;
    }
}
