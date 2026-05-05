package org.example.jwtjavaeight.service;

import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.dto.RefreshRequest;
import org.example.jwtjavaeight.domain.dto.RegisterRequest;

public interface AuthService {

  void register(RegisterRequest registerRequest);

  LoginResponse refresh(RefreshRequest refreshRequest);

  void logout(Long userId);

  void unlockUser(Long userId);
}
