package com.example.fleamarketsystem.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String toEmail, String subject, String message) {
        if (fromEmail == null || fromEmail.isEmpty()) {
            System.err.println("Gmail SMTP設定が不完全です。環境変数GMAIL_SMTP_USERNAMEを設定してください。");
            return;
        }

        if (toEmail == null || toEmail.isEmpty()) {
            System.err.println("送信先メールアドレスが指定されていません。");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(message, false); // false = plain text

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            System.err.println("メール送信失敗: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("メール送信エラー: " + e.getMessage());
        }
    }
}

