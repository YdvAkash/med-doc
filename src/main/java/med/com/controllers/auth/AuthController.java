package med.com.controllers.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import med.com.dtos.request.LoginRequest;
import med.com.dtos.request.RegisterRequest;
import med.com.dtos.request.UpdateProfileRequest;
import med.com.dtos.response.ApiResponse;
import med.com.dtos.response.LoginResponse;
import med.com.dtos.response.RegisterResponse;
import med.com.dtos.response.profileDTO;
import med.com.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, 201));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<profileDTO>> getProfile(Principal principal) {
        profileDTO profile = authService.getProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<profileDTO>> updateProfile(
            Principal principal,
            @RequestBody UpdateProfileRequest request
    ) {
        profileDTO updatedProfile = authService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile));
    }
}

