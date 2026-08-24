package med.com.services.impl;

import lombok.RequiredArgsConstructor;
import med.com.dtos.request.LoginRequest;
import med.com.dtos.request.RegisterRequest;
import med.com.dtos.request.UpdateProfileRequest;
import med.com.dtos.response.LoginResponse;
import med.com.dtos.response.RegisterResponse;
import med.com.dtos.response.UserDto;
import med.com.dtos.response.profileDTO;
import med.com.entity.UserEntity;
import med.com.repository.UserRepository;
import med.com.services.AuthService;
import med.com.services.JwtService;
import med.com.services.S3Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final S3Service s3Service;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isActive(true)
                .build();

        UserEntity savedUser = userRepository.save(user);

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userDto)
                .build();
    }

    @Override
    public profileDTO getProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return mapToProfileDto(user);
    }

    @Override
    public profileDTO updateProfile(String email, UpdateProfileRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getBloodGroup() != null) user.setBloodGroup(request.getBloodGroup());
        if (request.getEmergencyContactName() != null) user.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) user.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getChronicConditions() != null) user.setChronicConditions(request.getChronicConditions());
        if (request.getAllergies() != null) user.setAllergies(request.getAllergies());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getProfilePictureUrl() != null) user.setProfilePictureUrl(request.getProfilePictureUrl());

        userRepository.save(user);

        return mapToProfileDto(user);
    }

    @Override
    public profileDTO uploadProfilePicture(String email, MultipartFile file) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("Invalid file type. Only JPG and PNG are allowed");
        }

        // Delete old profile picture if exists and looks like an S3 key
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().startsWith("http")) {
            try {
                s3Service.deleteFile(user.getProfilePictureUrl());
            } catch (Exception e) {
                // Ignore if delete fails
            }
        }

        String extension = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".") 
            ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1) 
            : "jpg";
            
        String s3Key = "users/" + user.getId() + "/profile/" + UUID.randomUUID() + "." + extension;
        s3Service.uploadFile(file, s3Key);

        user.setProfilePictureUrl(s3Key);
        userRepository.save(user);

        return mapToProfileDto(user);
    }

    private profileDTO mapToProfileDto(UserEntity user) {
        return profileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role("USER")
                .bloodGroup(user.getBloodGroup())
                .emergencyContactName(user.getEmergencyContactName())
                .emergencyContactPhone(user.getEmergencyContactPhone())
                .chronicConditions(user.getChronicConditions())
                .allergies(user.getAllergies())
                .dateOfBirth(user.getDateOfBirth())
                .profilePictureUrl(getPresignedUrlOrRaw(user.getProfilePictureUrl()))
                .build();
    }

    private String getPresignedUrlOrRaw(String urlOrKey) {
        if (urlOrKey == null) return null;
        if (urlOrKey.startsWith("http://") || urlOrKey.startsWith("https://")) {
            return urlOrKey; // It's already a full URL
        }
        // Assume it's an S3 key
        try {
            return s3Service.getPresignedUrl(urlOrKey);
        } catch (Exception e) {
            return urlOrKey; // Fallback if S3 fails
        }
    }
}
