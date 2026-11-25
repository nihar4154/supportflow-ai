package io.github.nihar4154.supportflow.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.nihar4154.supportflow.agent.config.InboxProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SeedMailControllerTests {
  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final String inboxAddress = "help@example.org";
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new SeedMailController(sender, new InboxProperties(null, inboxAddress, 10000, 50)))
          .build();

  @Test
  void seedsDefaultMessage() throws Exception {
    mvc.perform(post("/seed-mail"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("sent"))
        .andExpect(jsonPath("$.from").value("customer@example.com"))
        .andExpect(jsonPath("$.to").value(inboxAddress))
        .andExpect(jsonPath("$.subject").value("Test support request"))
        .andExpect(jsonPath("$.body").value("Hi, I need help with my recent order."))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void sendsSuppliedFieldsToConfiguredInbox() throws Exception {
    mvc.perform(
            post("/seed-mail")
                .param("from", "buyer@example.org")
                .param("subject", "Order status")
                .param("body", "Has my order shipped?"))
        .andExpect(status().isOk());

    var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(message.capture());
    assertThat(message.getValue().getTo()).containsExactly(inboxAddress);
    assertThat(message.getValue().getFrom()).isEqualTo("buyer@example.org");
    assertThat(message.getValue().getSubject()).isEqualTo("Order status");
    assertThat(message.getValue().getText()).isEqualTo("Has my order shipped?");
  }

  @Test
  void reportsDeliveryFailure() throws Exception {
    doThrow(new MailSendException("SMTP unavailable"))
        .when(sender)
        .send(any(SimpleMailMessage.class));
    mvc.perform(post("/seed-mail"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value("failed"))
        .andExpect(jsonPath("$.error").value("SMTP unavailable"));
  }
}
