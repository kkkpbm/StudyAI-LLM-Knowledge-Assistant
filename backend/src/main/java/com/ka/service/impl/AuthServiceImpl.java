package com.ka.service.impl;

import com.ka.dto.LoginDTO;
import com.ka.dto.RegisterDTO;
import com.ka.entity.User;
import com.ka.common.BusinessException;
import com.ka.mapper.UserMapper;
import com.ka.security.JwtUtil;
import com.ka.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterDTO dto) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setRole("USER");
        userMapper.insert(user);
    }

    @Override
    public Map<String, String> login(LoginDTO dto) {
        // 先校验入参非空
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        // 统一错误提示，防止用户名枚举
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of("token", token, "username", user.getUsername(), "userId", String.valueOf(user.getId()));
    }
}
