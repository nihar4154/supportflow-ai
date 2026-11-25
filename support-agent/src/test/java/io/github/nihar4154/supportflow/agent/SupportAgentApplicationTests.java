package io.github.nihar4154.supportflow.agent;

import io.github.nihar4154.supportflow.agent.service.InboxMonitor;
import io.github.nihar4154.supportflow.agent.service.SupportAgent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.ai.mcp.client.enabled=false",
        "spring.docker.compose.enabled=false"
})
class SupportAgentApplicationTests {

    @MockitoBean
    private SupportAgent supportAgent;

    @MockitoBean
    private InboxMonitor inboxMonitor;

    @Test
    void contextLoads() {
    }
}
