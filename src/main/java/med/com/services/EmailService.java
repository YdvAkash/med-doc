package med.com.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("officeakashyadav@gmail.com");
        message.setTo(toEmail);

        if ("REGISTER".equals(purpose)) {
            message.setSubject("Verify Your Med-Doc Account");
            message.setText("Welcome to Med-Doc! Your OTP for registration is: " + otp + "\nThis OTP is valid for 10 minutes.");
        } else if ("FORGOT_PASSWORD".equals(purpose)) {
            message.setSubject("Reset Your Med-Doc Password");
            message.setText("You requested a password reset. Your OTP is: " + otp + "\nThis OTP is valid for 10 minutes. If you did not request this, please ignore this email.");
        }

        try {
            javaMailSender.send(message);
            log.info("OTP Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending OTP email to {}", toEmail, e);
        }
    }
}
