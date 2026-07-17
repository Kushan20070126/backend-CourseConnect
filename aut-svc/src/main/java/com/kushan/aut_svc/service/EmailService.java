package com.kushan.aut_svc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:no-reply@courseconnect.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendOtp(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject("Your CourseConnect verification code");
            message.setText("Welcome to CourseConnect!\n\nYour 6-digit verification code is: " + otp +
                    "\n\nThis code expires in 5 minutes. If you did not request this, you can safely ignore this email.");
            mailSender.send(message);
        } catch (Exception ex) {
            // Dev fallback: when no SMTP server is configured, print the code to the
            // console so the registration flow stays testable without real email.
            System.out.println("==== [DEV] OTP for " + to + " : " + otp + " ====");
        }
    }
}
