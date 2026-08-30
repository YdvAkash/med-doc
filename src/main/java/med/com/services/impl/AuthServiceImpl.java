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
import med.com.exceptions.BadRequestException;
import med.com.exceptions.DuplicateResourceException;
import med.com.exceptions.ResourceNotFoundException;
import med.com.exceptions.UnauthorizedException;
import med.com.repository.UserRepository;
import med.com.services.AuthService;
import med.com.services.JwtService;
import med.com.services.S3Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.Set;
import med.com.entity.OtpEntity;
import med.com.repository.OtpRepository;
import med.com.services.EmailService;
import med.com.dtos.request.VerifyOtpRequest;
import med.com.dtos.request.ForgotPasswordRequest;
import med.com.dtos.request.ResetPasswordRequest;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final S3Service s3Service;
    private final OtpRepository otpRepository;
    private final EmailService emailService;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");

    @Override
    public RegisterResponse register(RegisterRequest request) {
        java.util.Optional<UserEntity> existingUserOpt = userRepository.findByEmail(request.getEmail());
        UserEntity savedUser;

        if (existingUserOpt.isPresent()) {
            UserEntity existingUser = existingUserOpt.get();
            if (existingUser.getIsActive()) {
                throw new DuplicateResourceException("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
            } else {
                // User exists but is unverified, update their details and resend OTP
                existingUser.setFirstName(request.getFirstName());
                existingUser.setLastName(request.getLastName());
                existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                savedUser = userRepository.save(existingUser);
            }
        } else {
            UserEntity user = UserEntity.builder()
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .isActive(false) // Wait for OTP
                    .build();
            savedUser = userRepository.save(user);
        }

        // Generate and send OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        OtpEntity otpEntity = OtpEntity.builder()
                .email(savedUser.getEmail())
                .otp(otp)
                .purpose("REGISTER")
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();
        otpRepository.deleteByEmailAndPurpose(savedUser.getEmail(), "REGISTER");
        otpRepository.save(otpEntity);

        emailService.sendOtpEmail(savedUser.getEmail(), otp, "REGISTER");

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
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Incorrect email or password."));

        if (!user.getIsActive()) {
            // Generate and send OTP
            String otp = String.format("%06d", new Random().nextInt(999999));
            OtpEntity otpEntity = OtpEntity.builder()
                    .email(user.getEmail())
                    .otp(otp)
                    .purpose("REGISTER")
                    .expiryTime(LocalDateTime.now().plusMinutes(10))
                    .build();
            otpRepository.deleteByEmailAndPurpose(user.getEmail(), "REGISTER");
            otpRepository.save(otpEntity);

            emailService.sendOtpEmail(user.getEmail(), otp, "REGISTER");

            throw new UnauthorizedException("UNVERIFIED_ACCOUNT", "Account not verified. A new OTP has been sent to your email.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Incorrect email or password.");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .subscriptionTier(user.getSubscriptionTier())
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
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));
        
        return mapToProfileDto(user);
    }

    @Override
    public profileDTO updateProfile(String email, UpdateProfileRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

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
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User profile not found."));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("FILE_EMPTY", "Profile picture file cannot be empty.");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("INVALID_FILE_TYPE", "Invalid file type. Only JPG and PNG are allowed.");
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
                .subscriptionTier(user.getSubscriptionTier())
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

    @Override
    public void verifyRegistration(VerifyOtpRequest request) {
        OtpEntity otpEntity = otpRepository.findByEmailAndOtpAndPurpose(request.getEmail(), request.getOtp(), "REGISTER")
                .orElseThrow(() -> new BadRequestException("INVALID_OTP", "Incorrect OTP. Please try again."));

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP_EXPIRED", "This OTP has expired. Please request a new one.");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User account not found."));

        user.setIsActive(true);
        userRepository.save(user);
        otpRepository.delete(otpEntity);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("EMAIL_NOT_REGISTERED", "Please enter a registered email address."));

        String otp = String.format("%06d", new Random().nextInt(999999));
        OtpEntity otpEntity = OtpEntity.builder()
                .email(user.getEmail())
                .otp(otp)
                .purpose("FORGOT_PASSWORD")
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .build();
        
        // Remove any old OTPs for forgot password
        otpRepository.deleteByEmailAndPurpose(user.getEmail(), "FORGOT_PASSWORD");
        otpRepository.save(otpEntity);

        emailService.sendOtpEmail(user.getEmail(), otp, "FORGOT_PASSWORD");
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        OtpEntity otpEntity = otpRepository.findByEmailAndOtpAndPurpose(request.getEmail(), request.getOtp(), "FORGOT_PASSWORD")
                .orElseThrow(() -> new BadRequestException("INVALID_OTP", "Incorrect OTP. Please try again."));

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP_EXPIRED", "This OTP has expired. Please request a new one.");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User account not found."));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpRepository.delete(otpEntity);
    }

    @Override
    public void updatePushToken(String email, String token) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User account not found."));
        user.setPushToken(token);
        userRepository.save(user);
    }
}
