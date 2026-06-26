package com.smartparking.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public PasswordResetEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                     @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    public boolean sendResetLink(String email, String resetLink) {
        return send(email, "ParkingSmart - Reset password", "Click this link to reset your password: " + resetLink);
    }

    public boolean sendOtp(String email, String subject, String otp, int expiresInMinutes) {
        return send(email, subject,
                "Your ParkingSmart OTP is: " + otp + "\nThis code expires in " + expiresInMinutes + " minutes.");
    }

    private boolean send(String email, String subject, String text) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || fromAddress.isBlank()) {
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
