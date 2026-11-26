package io.github.nihar4154.supportflow.agent.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import java.net.http.HttpRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class McpTracingConfiguration {
  @Bean
  McpSyncHttpClientRequestCustomizer propagateMcpTrace(Tracer tracer, Propagator propagator) {
    return (request, method, endpoint, body, context) -> {
      var span = tracer.currentSpan();
      if (span != null) {
        propagator.inject(span.context(), request, HttpRequest.Builder::setHeader);
      }
    };
  }
}
