package com.example.cloud_share_api.email;

import lombok.Getter;

public enum MailTemplate {
  EMAIL_VERIFICATION("mail_email_verification.html", "Verify your Email Account."),
  RESET_PASSWORD("mail_reset_password.html", "Reset Password Request.");

  @Getter
  private String template;
  @Getter
  private String subject;

  MailTemplate(String template, String subject) {
    this.template = template;
    this.subject = subject;
  }
}
