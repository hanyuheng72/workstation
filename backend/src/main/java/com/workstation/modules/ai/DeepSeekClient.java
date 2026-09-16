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
        return complete(messages, jsonMode, jsonMode ? 0.1 : 0.7);
    }

    /**
     * @param temperature 采样温度。默认很低以保证输出稳定，但模型偶尔会退化成
     *                    只吐一片空格——那种情况下必须换一个温度重试，否则同样的
     *                    请求大概率得到同样的退化输出。
     */
    public Completion complete(List<ChatMessage> messages, boolean jsonMode, double temperature) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.model());
        body.put("temperature", temperature);
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
            JsonNode choice = choices.get(0);
            String content = choice.path("message").path("content").asText("");
            if (content.isBlank()) {
                // 偶发退化：结构合法但正文为空。把 finish_reason 与用量一起记下来，
                // 否则只能猜是超长、限流还是别的
                log.warn("DeepSeek 返回空正文 finish_reason={} usage={} 完整响应={}",
                        choice.path("finish_reason").asText(""),
                        root.path("usage"),
                        forLog(raw));
            }
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

    /** 转义成纯 ASCII，日志不受控制台编码影响，也能反查原文 */
    private static String forLog(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0x20 && c < 0x7F) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }

    public record Completion(String content, int promptTokens, int completionTokens) {
    }
}
