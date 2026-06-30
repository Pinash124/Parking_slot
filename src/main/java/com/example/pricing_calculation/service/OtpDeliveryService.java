package com.example.pricing_calculation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OtpDeliveryService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean exposeOtp;

    public OtpDeliveryService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${parking.auth.expose-development-otp:true}") boolean exposeOtp) {
        this.mailSender = mailSender;
        this.exposeOtp = exposeOtp;
    }

    public void send(String email, String otp, String purpose) {

        JavaMailSender sender = mailSender.getIfAvailable();

        if (sender == null) {
            log.warn("JavaMailSender is not available. Check MAIL_HOST/MAIL_USERNAME/MAIL_PASSWORD.");
            if (!exposeOtp) {
                throw new BadRequestException("Unable to send OTP email");
            }
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("dangnguyenquocthong7@gmail.com");
            message.setTo(email);
            message.setSubject("Smart Parking OTP - " + purpose);
            message.setText("Your OTP is " + otp + ". It expires in 5 minutes.");

            long start = System.currentTimeMillis();
            log.info("Start sending OTP email to {}", email);

            sender.send(message);

            long end = System.currentTimeMillis();

            log.info("Mail send took {} ms", (end - start));
            log.info("OTP email sent successfully to {} for purpose {}", email, purpose);

        } catch (RuntimeException ex) {
            log.error("Failed to send OTP email to {} for purpose {}", email, purpose, ex);

            if (!exposeOtp) {
                throw new BadRequestException("Unable to send OTP email");
            }
        }
    }

    public String developmentOtp(String otp) {
        return exposeOtp ? otp : null;
    }
}