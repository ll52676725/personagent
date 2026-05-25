package com.example.agentplatform.platform.controller;

import com.example.agentplatform.common.config.JwtTokenProvider;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.platform.dto.*;
import com.example.agentplatform.platform.entity.User;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.platform.repository.UserRepository;
import com.example.agentplatform.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    
    @PostMapping("/register")
    public ResponseEntity<Result<User>> register(@Valid @RequestBody UserRegisterDTO dto) {
        User user = userService.register(dto);
        return ResponseEntity.ok(Result.success(user));
    }
    
    @PostMapping("/login")
    public ResponseEntity<Result<LoginResult>> login(@Valid @RequestBody LoginDTO dto) {
        LoginResult result = userService.login(dto);
        return ResponseEntity.ok(Result.success(result));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<Result<LoginResult>> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        String refreshToken = dto.getRefreshToken();
        
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("无效的refreshToken");
        }
        
        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        
        String newAccessToken = tokenProvider.generateToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);
        
        LoginResult result = LoginResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(tokenProvider.getExpiresIn())
                .user(LoginResult.UserInfo.fromUser(user))
                .build();
        
        return ResponseEntity.ok(Result.success(result));
    }
    
    @GetMapping("/me")
    public ResponseEntity<Result<LoginResult.UserInfo>> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(Result.success(LoginResult.UserInfo.fromUser(user)));
    }
}