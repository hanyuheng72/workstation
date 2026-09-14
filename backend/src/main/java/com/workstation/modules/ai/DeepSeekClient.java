package com.workstation.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workstation.common.config.DeepSeekProperties;
import com.workstation.common.exception.BusinessException;
import com.workstation.common.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * DeepSeek 接口封装。
 *
 * 安全约定：API Key 只在本类里读取并放进请求头，
 * 不写入任何日志、不出现在任何返回值里。出错信息也只描述状态码，不回显请求内容。
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DeepSeekClient(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        // 生成总结这类请求模型要想一会儿，读超时给足
        factory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public String model() {
        return properties.model();
    }

    /**
     * @param jsonMode 要求模型只输出 JSON 对象（DeepSeek 的 response_format）
     */
    public Completion complete(List<ChatMessage> messages, boolean jsonMode) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.model());
        body.put("temperature", jsonMode ? 0.1 : 0.7);
        body.put("max_tokens", 1200);
        body.put("stream", false);

        ArrayNode array = body.putArray("messages");
        for (ChatMessage message : messages) {
            ObjectNode node = array.addObject();
            node.put("role", message.role());
            node.put("content", message.content());
        }
        if (jsonMode) {
            body.putObject("response_format").put("type", "json_object");
        }

        String raw;
        try {
            raw = restClient.post()
                    .uri(properties.chatCompletionsUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            // 只记录异常类型与消息，异常消息里不含请求体，因此不会带出 Key
            log.warn("DeepSeek 调用失败: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_CALL_FAILED, "调用 AI 服务失败，请稍后再试");
        }

        return parse(raw);
    }

    private Completion parse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_CALL_FAILED, "AI 没有返回内容");
            }
            String content = choices.get(0).path("message").path("content").asText("");
            JsonNode usage = root.path("usage");
            return new Completion(
                    content,
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("DeepSeek 响应解析失败: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.AI_CALL_FAILED, "AI 返回的内容无法解析");
        }
    }

    public record Completion(String content, int promptTokens, int completionTokens) {
    }
}
