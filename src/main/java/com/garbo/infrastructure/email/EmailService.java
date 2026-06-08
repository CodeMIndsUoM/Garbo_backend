package com.garbo.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendAdminCredentials(String toEmail, String tempPassword) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        if (fromAddress != null && !fromAddress.isBlank()) {
            helper.setFrom(fromAddress);
        }
        helper.setTo(toEmail);
        helper.setSubject("Your Garbo account credentials");
        helper.setText(
                AdminCredentialsEmailTemplate.buildPlainText(toEmail, tempPassword),
                AdminCredentialsEmailTemplate.buildHtml(toEmail, tempPassword));

        mailSender.send(message);
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
