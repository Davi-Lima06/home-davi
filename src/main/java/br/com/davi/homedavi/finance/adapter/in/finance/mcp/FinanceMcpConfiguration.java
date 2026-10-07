package br.com.davi.homedavi.finance.adapter.in.finance.mcp;

import br.com.davi.homedavi.finance.adapter.in.integration.mcp.PluggyQueryMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra as tools do tópico finance no servidor MCP. Outros tópicos declaram o seu próprio
 * provider.
 */
@Configuration
public class FinanceMcpConfiguration {
  @Bean
  ToolCallbackProvider financeToolCallbackProvider(
      FinanceMcpTools financeMcpTools, PluggyQueryMcpTools pluggyQueryMcpTools) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(financeMcpTools, pluggyQueryMcpTools)
        .build();
  }
}
