package com.edusphere.identity.auth.service;

import com.edusphere.identity.auth.dto.LoginRequest;
import com.edusphere.identity.auth.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
