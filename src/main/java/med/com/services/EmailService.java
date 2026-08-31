package med.com.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    public void sendSubscriptionSuccessEmail(String toEmail, String name, String plan) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String htmlMsg = "<div style=\"font-family: Arial, sans-serif; text-align: center; color: #333;\">" +
                    "<h2 style=\"color: #0d6efd;\">Welcome to the " + plan + " Plan!</h2>" +
                    "<p>Hi <b>" + name + "</b>,</p>" +
                    "<p>Thank you for upgrading your Mediva subscription. We are thrilled to have you on board!</p>" +
                    "<p>You now have access to premium features designed to give you the best experience.</p>" +
                    "<br>" +
                    "<p>Stay healthy,</p>" +
                    "<p><b>The Mediva Team</b></p>" +
                    "</div>";

            helper.setText(htmlMsg, true);
            helper.setTo(toEmail);
            helper.setSubject("Mediva - Subscription Successful!");
            helper.setFrom("officeakashyadav@gmail.com");

            javaMailSender.send(mimeMessage);
            log.info("Subscription Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending Subscription email to {}", toEmail, e);
        }
    }
}
