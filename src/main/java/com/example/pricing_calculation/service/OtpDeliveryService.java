package com.example.pricing_calculation.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpDeliveryService {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean exposeOtp;
    public OtpDeliveryService(ObjectProvider<JavaMailSender> mailSender,
            @Value("${parking.auth.expose-development-otp:true}") boolean exposeOtp) {
        this.mailSender = mailSender;
        this.exposeOtp = exposeOtp;
    }
    public void send(String email, String otp, String purpose) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Smart Parking OTP - " + purpose);
            message.setText("Your OTP is " + otp + ". It expires in 5 minutes.");
            sender.send(message);
        } catch (RuntimeException ignored) {
            if (!exposeOtp) throw new BadRequestException("Unable to send OTP email");
        }
    }
    public String developmentOtp(String otp) { return exposeOtp ? otp : null; }
}
