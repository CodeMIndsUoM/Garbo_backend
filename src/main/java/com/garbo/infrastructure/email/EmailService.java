package com.garbo.infrastructure.email;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAdminCredentials(String toEmail, String tempPassword) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Your Admin Account Credentials");
        StringBuilder body = new StringBuilder();
        body.append("Hello,").append(System.lineSeparator()).append(System.lineSeparator());
        body.append("Your admin account has been created.").append(System.lineSeparator());
        body.append("Email: ").append(toEmail == null ? "" : toEmail).append(System.lineSeparator());
        body.append("Temporary password: ").append(tempPassword == null ? "" : tempPassword)
                .append(System.lineSeparator()).append(System.lineSeparator());
        body.append("Please change your password after first login.");
        msg.setText(body.toString());

        mailSender.send(msg);
    }
}
