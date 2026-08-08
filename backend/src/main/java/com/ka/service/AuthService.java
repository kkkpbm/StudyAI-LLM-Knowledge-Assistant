package com.ka.service;

import com.ka.dto.LoginDTO;
import com.ka.dto.RegisterDTO;

import java.util.Map;

public interface AuthService {
    void register(RegisterDTO dto);

    Map<String, String> login(LoginDTO dto);
}
