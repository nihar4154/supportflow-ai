package io.github.nihar4154.supportflow.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class McpRequestLogFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(McpRequestLogFilter.class);

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !"/mcp".equals(request.getServletPath());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    log.info("Received MCP {} request", request.getMethod());
    chain.doFilter(request, response);
  }
}
