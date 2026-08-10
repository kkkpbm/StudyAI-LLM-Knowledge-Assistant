package com.ka.module.auth.service;

import com.ka.module.auth.domain.LoginDTO;
import com.ka.module.auth.domain.RegisterDTO;

import java.util.Map;

public interface AuthService {
    void register(RegisterDTO dto);

    Map<String, String> login(LoginDTO dto);
}

