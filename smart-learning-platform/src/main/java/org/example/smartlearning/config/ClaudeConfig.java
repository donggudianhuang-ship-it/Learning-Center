package org.example.smartlearning.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Claude API配置类
 */
@Configuration
public class ClaudeConfig {

    @Value("${claude.api-key}")
    private String apiKey;

    @Value("${claude.model}")
    private String model;

    @Value("${claude.max-tokens}")
    private Integer maxTokens;

    @Value("${claude.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Bean
    public ClaudeProperties claudeProperties() {
        return new ClaudeProperties(resolveApiKey(), model, maxTokens, apiUrl);
    }

    public record ClaudeProperties(String apiKey, String model, Integer maxTokens, String apiUrl) {}

    private String resolveApiKey() {
        String localConfigApiKey = readLocalConfigApiKey();
        if (hasText(localConfigApiKey)) {
            return localConfigApiKey.trim();
        }
        if (hasText(apiKey)) {
            return apiKey.trim();
        }
        String envApiKey = System.getenv("DEEPSEEK_API_KEY");
        return hasText(envApiKey) ? envApiKey.trim() : apiKey;
    }

    private String readLocalConfigApiKey() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("config").resolve("application.yml");
            if (Files.isRegularFile(candidate)) {
                try {
                    YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
                    yaml.setResources(new FileSystemResource(candidate));
                    Properties properties = yaml.getObject();
                    if (properties != null) {
                        return properties.getProperty("claude.api-key");
                    }
                } catch (Exception ignored) {
                    return null;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
