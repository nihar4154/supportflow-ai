package io.github.nihar4154.supportflow.agent.controller;

import io.github.nihar4154.supportflow.agent.config.InboxProperties;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Sends a sample customer email to the monitored inbox for the local demo. */
@RestController
public class SeedMailController {
  private final JavaMailSender mailSender;
  private final InboxProperties inbox;

  public SeedMailController(JavaMailSender mailSender, InboxProperties inbox) {
    this.mailSender = mailSender;
    this.inbox = inbox;
  }

  @PostMapping("/seed-mail")
  public ResponseEntity<SeedResult> seed(
      @RequestParam(defaultValue = "customer@example.com") String from,
      @RequestParam(defaultValue = "Test support request") String subject,
      @RequestParam(defaultValue = "Hi, I need help with my recent order.") String body) {
    String to = inbox.address();
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setFrom(from);
    mail.setTo(to);
    mail.setSubject(subject);
    mail.setText(body);

    try {
      mailSender.send(mail);
    } catch (MailException e) {
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(
              new SeedResult(
                  "failed",
                  "Could not deliver email to " + to + ". Is Mailpit running?",
                  from,
                  to,
                  subject,
                  null,
                  e.getMessage(),
                  Instant.now()));
    }
    return ResponseEntity.ok(
        new SeedResult(
            "sent",
            "Email delivered to " + to + " for the next inbox poll.",
            from,
            to,
            subject,
            body,
            null,
            Instant.now()));
  }

  public record SeedResult(
      String status,
      String message,
      String from,
      String to,
      String subject,
      String body,
      String error,
      Instant timestamp) {}
}
