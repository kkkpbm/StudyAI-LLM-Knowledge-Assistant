package com.ka.controller;

import com.ka.common.Result;
import com.ka.dto.LoginDTO;
import com.ka.dto.RegisterDTO;
import com.ka.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }
}
