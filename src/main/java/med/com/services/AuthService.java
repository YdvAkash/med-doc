package med.com.services;

import med.com.dtos.request.RegisterRequest;
import med.com.dtos.response.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    med.com.dtos.response.LoginResponse login(med.com.dtos.request.LoginRequest request);
}
