package com.example.cloud_share_api.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Value("${spring.mail.username}")
  private String smtpUser;

  @Async
  public void sendMail(Mail mail) {
    MimeMessage message = mailSender.createMimeMessage(); 
    MimeMessageHelper messageHelper = new MimeMessageHelper(message);

    try {
      messageHelper.setFrom(smtpUser);
      messageHelper.setTo(mail.recipent());
      messageHelper.setSubject(mail.template().getSubject());
      messageHelper.setText(buildContext(mail), true);

      mailSender.send(message);
    } catch (Exception e) {
      log.error("Error sending mail...");
      e.printStackTrace();
    }
  }

  private String buildContext(Mail mail) {
    Context context = new Context();
    context.setVariable("args", mail.args());
    return templateEngine.process(mail.template().getTemplate(), context);
  }
}
