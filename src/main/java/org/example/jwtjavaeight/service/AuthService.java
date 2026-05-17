package org.example.jwtjavaeight.service;

import org.example.jwtjavaeight.domain.dto.LoginResponse;
import org.example.jwtjavaeight.domain.dto.RefreshRequest;

public interface AuthService {

  LoginResponse refresh(RefreshRequest refreshRequest);

  void logout(Long userId);
}
