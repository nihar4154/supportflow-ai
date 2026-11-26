package io.github.nihar4154.supportflow.mcp.tool;

import io.github.nihar4154.supportflow.mcp.dto.SupportDtos.*;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Configuration(proxyBeanMethods = false)
class McpToolLoggingConfiguration {
  private static final Logger log = LoggerFactory.getLogger(McpToolLoggingConfiguration.class);

  private static final Set<String> LOGGED_ARGUMENTS =
      Set.of("orderNumber", "sku", "intent", "sentiment", "refundType");

  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  static Advisor mcpToolLogging(ObjectProvider<ObservationRegistry> registry) {
    MethodInterceptor interceptor =
        invocation -> {
          String tool = invocation.getMethod().getAnnotation(McpTool.class).name();
          Observation call =
              Observation.createNotStarted("mcp.tool.call", registry.getObject())
                  .contextualName("mcp.tool " + tool)
                  .lowCardinalityKeyValue("tool", tool)
                  .start();
          Map<String, Object> details = new LinkedHashMap<>();
          var parameters = invocation.getMethod().getParameters();
          for (int i = 0; i < parameters.length; i++) {
            String name = parameters[i].getName();
            Object value = invocation.getArguments()[i];
            if (LOGGED_ARGUMENTS.contains(name) && value != null) {
              details.put(name, value.toString());
            }
          }
          long start = System.nanoTime();
          try (var scope = call.openScope()) {
            record(call, log.atInfo(), tool, details, "MCP tool started");
            try {
              Object result = invocation.proceed();
              addResult(details, result);
              finish(call, log.atInfo(), tool, details, start, "success");
              return result;
            } catch (Throwable failure) {
              call.error(failure);
              details.put("errorType", failure.getClass().getSimpleName());
              finish(call, log.atWarn(), tool, details, start, "error");
              throw failure;
            }
          } finally {
            call.stop();
          }
        };
    return new DefaultPointcutAdvisor(
        AnnotationMatchingPointcut.forMethodAnnotation(McpTool.class), interceptor);
  }

  private static void finish(
      Observation call,
      LoggingEventBuilder event,
      String tool,
      Map<String, Object> details,
      long start,
      String outcome) {
    call.lowCardinalityKeyValue("outcome", outcome);
    details.put("durationMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    record(
        call, event.addKeyValue("outcome", outcome), tool, details, "MCP tool execution completed");
  }

  private static void record(
      Observation call,
      LoggingEventBuilder event,
      String tool,
      Map<String, Object> details,
      String message) {
    for (var field : details.entrySet()) {
      Object value = field.getValue();
      if (value != null) {
        event = event.addKeyValue(field.getKey(), value);
        call.highCardinalityKeyValue(field.getKey(), value.toString());
      }
    }
    call.event(Observation.Event.of(message));
    event.addKeyValue("tool", tool).log(message);
  }

  private static void addResult(Map<String, Object> details, Object result) {
    switch (result) {
      case CustomerInfo c -> details.put("customerId", c.id());
      case ProductInfo p -> {
        details.put("sku", p.sku());
        details.put("stockQuantity", p.stockQuantity());
      }
      case OrderDetails o -> {
        details.put("orderNumber", o.orderNumber());
        details.put("status", o.status());
        details.put("orderTotal", o.totalAmount());
        details.put("currency", o.currency());
        details.put("itemCount", o.items().size());
        details.put("paymentStatuses", o.payments().stream().map(PaymentInfo::status).toList());
      }
      case DuplicateChargeResult d -> {
        details.put("orderNumber", d.orderNumber());
        details.put("duplicateDetected", d.duplicateDetected());
        details.put("chargeCount", d.chargeCount());
        details.put("totalCharged", d.totalCharged());
        details.put("expectedAmount", d.expectedAmount());
        details.put("overchargedAmount", d.overchargedAmount());
      }
      case WarrantyStatus w -> {
        details.put("inWarranty", w.inWarranty());
        details.put("sku", w.sku());
        details.put("warrantyEndDate", w.warrantyEndDate().toString());
      }
      case TicketHistory h -> details.put("ticketCount", h.totalTickets());
      case RefundResult r -> {
        details.put("refundId", r.refundId());
        details.put("amount", r.amount());
        details.put("currency", r.currency());
        details.put("status", r.status());
      }
      case TicketLogResult t -> {
        details.put("ticketId", t.ticketId());
        details.put("status", t.status());
      }
      case Collection<?> items -> details.put("resultCount", items.size());
      case null, default -> {}
    }
  }
}
