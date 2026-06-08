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
        msg.setSubject("Your Account Credentials");
        StringBuilder body = new StringBuilder();
        body.append("Hello,").append(System.lineSeparator()).append(System.lineSeparator());
        body.append("Your account has been created.").append(System.lineSeparator());
        body.append("Email: ").append(toEmail == null ? "" : toEmail).append(System.lineSeparator());
        body.append("Temporary password: ").append(tempPassword == null ? "" : tempPassword)
                .append(System.lineSeparator()).append(System.lineSeparator());
        body.append("Please change your password after first login.");
        msg.setText(body.toString());

        mailSender.send(msg);
    }

    public void sendRegistrationApproved(String toEmail, String name) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Garbo Registration Approved");
        StringBuilder body = new StringBuilder();
        body.append("Hello").append(name != null && !name.isBlank() ? " " + name : "")
                .append(",").append(System.lineSeparator()).append(System.lineSeparator());
        body.append("Your third-party collector registration has been approved.")
                .append(System.lineSeparator());
        body.append("Open the Garbo app and set your password to start using your account.")
                .append(System.lineSeparator());
        msg.setText(body.toString());
        mailSender.send(msg);
    }

    public void sendRegistrationRejected(String toEmail, String name, String reason) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Garbo Registration Update");
        StringBuilder body = new StringBuilder();
        body.append("Hello").append(name != null && !name.isBlank() ? " " + name : "")
                .append(",").append(System.lineSeparator()).append(System.lineSeparator());
        body.append("Your third-party collector registration was not approved at this time.")
                .append(System.lineSeparator());
        if (reason != null && !reason.isBlank()) {
            body.append("Reason: ").append(reason.trim()).append(System.lineSeparator());
        }
        body.append(System.lineSeparator());
        body.append("Contact your council admin if you have questions.");
        msg.setText(body.toString());
        mailSender.send(msg);
    }
}
