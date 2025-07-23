package com.midlane.project_management_tool_auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from:noreply@projectmanagement.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String verificationLink) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email Address - Project Management Tool");

            String htmlContent = buildVerificationEmailTemplate(verificationLink, toEmail);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Project Management Tool!");

            String htmlContent = buildWelcomeEmailTemplate(toEmail);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password - Project Management Tool");

            String htmlContent = buildPasswordResetEmailTemplate(resetLink, toEmail);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Async
    public void sendSimpleEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);

            javaMailSender.send(message);
            log.info("Simple email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildVerificationEmailTemplate(String verificationLink, String email) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Verify Your Email</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 15px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎯 Project Management Tool</h1>
                        <p>Verify Your Email Address</p>
                    </div>
                    <div class="content">
                        <h2>Welcome aboard!</h2>
                        <p>Hi there!</p>
                        <p>Thank you for registering with Project Management Tool. To complete your registration and start managing your projects, please verify your email address by clicking the button below:</p>
                        
                        <div style="text-align: center;">
                            <a href="VERIFICATION_LINK_PLACEHOLDER" class="button">✅ Verify Email Address</a>
                        </div>
                        
                        <p>If the button doesn't work, you can also copy and paste this link into your browser:</p>
                        <p style="background: #e9e9e9; padding: 10px; border-radius: 5px; word-break: break-all;">VERIFICATION_LINK_PLACEHOLDER</p>
                        
                        <p><strong>⏰ This verification link will expire in 24 hours.</strong></p>
                        
                        <p>If you didn't create an account with us, please ignore this email.</p>
                        
                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #ddd;">
                        
                        <h3>🚀 What's Next?</h3>
                        <ul>
                            <li>Create and manage projects</li>
                            <li>Collaborate with team members</li>
                            <li>Track progress and deadlines</li>
                            <li>Organize tasks efficiently</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>© 2025 Project Management Tool. All rights reserved.</p>
                        <p>This is an automated email, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return template.replace("VERIFICATION_LINK_PLACEHOLDER", verificationLink);
    }

    private String buildWelcomeEmailTemplate(String email) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Project Management Tool</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #28a745 0%, #20c997 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 15px 30px; background: #28a745; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .feature { background: white; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #28a745; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Welcome to Project Management Tool!</h1>
                        <p>Your email has been verified successfully</p>
                    </div>
                    <div class="content">
                        <h2>🎯 You're All Set!</h2>
                        <p>Congratulations! Your email address has been verified and your account is now fully activated.</p>
                        
                        <div style="text-align: center;">
                            <a href="FRONTEND_URL_PLACEHOLDER" class="button">🚀 Start Managing Projects</a>
                        </div>
                        
                        <h3>🌟 What You Can Do Now:</h3>
                        
                        <div class="feature">
                            <h4>📋 Create Projects</h4>
                            <p>Organize your work into projects and break them down into manageable tasks.</p>
                        </div>
                        
                        <div class="feature">
                            <h4>👥 Collaborate</h4>
                            <p>Invite team members and work together efficiently on shared projects.</p>
                        </div>
                        
                        <div class="feature">
                            <h4>📊 Track Progress</h4>
                            <p>Monitor project progress with visual dashboards and real-time updates.</p>
                        </div>
                        
                        <div class="feature">
                            <h4>⏰ Meet Deadlines</h4>
                            <p>Set deadlines, get reminders, and never miss important milestones.</p>
                        </div>
                        
                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #ddd;">
                        
                        <p><strong>Need Help?</strong></p>
                        <p>If you have any questions or need assistance, feel free to reach out to our support team.</p>
                        
                        <p>Happy project managing! 🎯</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Project Management Tool. All rights reserved.</p>
                        <p>This is an automated email, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return template.replace("FRONTEND_URL_PLACEHOLDER", frontendUrl);
    }

    private String buildPasswordResetEmailTemplate(String resetLink, String email) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Reset Your Password</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #dc3545 0%, #fd7e14 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 15px 30px; background: #dc3545; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .warning { background: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Project Management Tool</h1>
                        <p>Password Reset Request</p>
                    </div>
                    <div class="content">
                        <h2>Reset Your Password</h2>
                        <p>Hi there!</p>
                        <p>We received a request to reset your password for your Project Management Tool account. If you made this request, click the button below to reset your password:</p>
                        
                        <div style="text-align: center;">
                            <a href="RESET_LINK_PLACEHOLDER" class="button">🔑 Reset Password</a>
                        </div>
                        
                        <p>If the button doesn't work, you can also copy and paste this link into your browser:</p>
                        <p style="background: #e9e9e9; padding: 10px; border-radius: 5px; word-break: break-all;">RESET_LINK_PLACEHOLDER</p>
                        
                        <div class="warning">
                            <h4>⚠️ Important Security Information:</h4>
                            <ul>
                                <li>This reset link will expire in 1 hour for your security</li>
                                <li>If you didn't request this reset, please ignore this email</li>
                                <li>Your current password will remain unchanged until you complete the reset</li>
                            </ul>
                        </div>
                        
                        <h3>🛡️ Security Tips:</h3>
                        <ul>
                            <li>Use a strong, unique password</li>
                            <li>Include uppercase, lowercase, numbers, and symbols</li>
                            <li>Don't reuse passwords from other accounts</li>
                            <li>Consider using a password manager</li>
                        </ul>
                        
                        <p>If you continue to have problems, please contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Project Management Tool. All rights reserved.</p>
                        <p>This is an automated email, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return template.replace("RESET_LINK_PLACEHOLDER", resetLink);
    }
}
