package com.vicente.storage.service;

import com.vicente.storage.dto.AuthRequestDTO;

public interface AuthService {
    Long authenticate(AuthRequestDTO authRequestDTO);
}
