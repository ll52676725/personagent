package com.example.agentplatform.platform.service;

import com.example.agentplatform.platform.dto.LoginDTO;
import com.example.agentplatform.platform.dto.LoginResult;
import com.example.agentplatform.platform.dto.UserRegisterDTO;
import com.example.agentplatform.platform.entity.User;

public interface UserService {
    User register(UserRegisterDTO dto);
    LoginResult login(LoginDTO dto);
    User getCurrentUser();
    User getUserById(Long userId);
}