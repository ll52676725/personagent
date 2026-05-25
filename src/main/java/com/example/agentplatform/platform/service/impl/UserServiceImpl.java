package com.example.agentplatform.platform.service.impl;

import com.example.agentplatform.common.config.JwtTokenProvider;
import com.example.agentplatform.platform.dto.LoginDTO;
import com.example.agentplatform.platform.dto.LoginResult;
import com.example.agentplatform.platform.dto.UserRegisterDTO;
import com.example.agentplatform.platform.entity.User;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.common.exception.UnauthorizedException;
import com.example.agentplatform.platform.repository.UserRepository;
import com.example.agentplatform.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    @Override
    @Transactional
    public User register(UserRegisterDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("邮箱已存在");
        }
        
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .status(1)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("用户注册成功: {}", savedUser.getUsername());
        return savedUser;
    }
    
    @Override
    public LoginResult login(LoginDTO dto) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }
        
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        
        String token = tokenProvider.generateToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);
        
        return LoginResult.builder()
                .accessToken(token)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getExpiresIn())
                .user(LoginResult.UserInfo.fromUser(user))
                .build();
    }
    
    @Override
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new UnauthorizedException("未登录");
    }
    
    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }
}