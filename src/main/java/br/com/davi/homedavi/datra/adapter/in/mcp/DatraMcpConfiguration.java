package br.com.davi.homedavi.datra.adapter.in.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatraMcpConfiguration {
  @Bean
  ToolCallbackProvider datraToolCallbackProvider(DatraMcpTools datraMcpTools) {
    return MethodToolCallbackProvider.builder().toolObjects(datraMcpTools).build();
  }
}
