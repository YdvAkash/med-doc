package med.com.services;

import med.com.dtos.request.LoginRequest;
import med.com.dtos.request.RegisterRequest;
import med.com.dtos.request.UpdateProfileRequest;
import med.com.dtos.response.LoginResponse;
import med.com.dtos.response.RegisterResponse;
import med.com.dtos.response.profileDTO;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    profileDTO getProfile(String email);
    profileDTO updateProfile(String email, UpdateProfileRequest request);
    profileDTO uploadProfilePicture(String email, org.springframework.web.multipart.MultipartFile file);
    
    void verifyRegistration(med.com.dtos.request.VerifyOtpRequest request);
    void forgotPassword(med.com.dtos.request.ForgotPasswordRequest request);
    void resetPassword(med.com.dtos.request.ResetPasswordRequest request);
}
