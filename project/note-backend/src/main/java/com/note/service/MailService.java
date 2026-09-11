package com.note.service;

import com.note.enums.ErrorCode;
import com.note.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final org.springframework.core.env.Environment env;

    public MailService(JavaMailSender mailSender, org.springframework.core.env.Environment env) {
        this.mailSender = mailSender;
        this.env = env;
    }

    @Async
    public void sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(env.getProperty("spring.mail.username"));
            helper.setTo(to);
            helper.setSubject("笔记共享平台 - 验证码");
            helper.setText(buildContent(code), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String buildContent(String code) {
        return """
            <div style="padding: 20px; font-family: sans-serif;">
                <h2 style="color: #333;">笔记共享平台</h2>
                <p>您的验证码是：</p>
                <div style="font-size: 32px; font-weight: bold; color: #409EFF; letter-spacing: 5px; margin: 10px 0;">%s</div>
                <p style="color: #999;">验证码有效期为5分钟，请勿泄露给他人。</p>
            </div>
            """.formatted(code);
    }
}
