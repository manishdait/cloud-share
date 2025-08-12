package com.example.cloud_share_api.email;

import lombok.Getter;

public enum MailTemplate {
  EMAIL_VERIFICATION("mail_email_verification.html", "Verify your Email Account.");

  @Getter
  private String template;
  @Getter
  private String subject;

  MailTemplate(String template, String subject) {
    this.template = template;
    this.subject = subject;
  }
}
