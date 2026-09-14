package com.workstation.modules.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workstation.common.exception.BusinessException;
import com.workstation.modules.ai.dto.AiMessageVO;
import com.workstation.modules.ai.dto.ChatRequest;
import com.workstation.modules.ai.dto.ChatResponse;
import com.workstation.modules.ai.dto.ConversationVO;
import com.workstation.modules.ai.entity.ActionStatus;
import com.workstation.modules.ai.entity.AiConversation;
import com.workstation.modules.ai.entity.AiMessage;
import com.workstation.modules.ai.entity.AiRole;
import com.workstation.modules.ai.mapper.AiConversationMapper;
import com.workstation.modules.ai.mapper.AiMessageMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 对话与「解析 → 草稿 → 确认 → 落库」这条链路。
 */
@Service
public class AiChatService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TITLE_MAX = 30;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final IntentParser intentParser;
    private final ActionExecutor actionExecutor;
    private final ObjectMapper objectMapper;

    public AiChatService(AiConversationMapper conversationMapper, AiMessageMapper messageMapper,
                         IntentParser intentParser, ActionExecutor actionExecutor,
                         ObjectMapper objectMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.intentParser = intentParser;
        this.actionExecutor = actionExecutor;
        this.objectMapper = objectMapper;
    }

    public List<ConversationVO> listConversations() {
        return conversationMapper.selectList(
                        Wrappers.lambdaQuery(AiConversation.class)
                                .orderByDesc(AiConversation::getUpdatedAt)
                                .last("LIMIT 50"))
                .stream()
                .map(item -> new ConversationVO(item.getId(), item.getTitle(),
                        item.getUpdatedAt() == null ? null : item.getUpdatedAt().format(TIME)))
                .toList();
    }

    public List<AiMessageVO> listMessages(Long conversationId) {
        requireConversation(conversationId);
        return messageMapper.selectList(
                        Wrappers.lambdaQuery(AiMessage.class)
                                .eq(AiMessage::getConversationId, conversationId)
                                .orderByAsc(AiMessage::getId))
                .stream().map(this::toVO).toList();
    }

    /**
     * 发一句话：存下来 → 交给模型解析 → 若识别出动作，存成 PENDING 草稿。
     * 注意这里**没有任何业务写入**，用户确认之前不会动业务表。
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        Long conversationId = request.conversationId();
        AiConversation conversation = conversationId == null
                ? createConversation(request.message())
                : requireConversation(conversationId);

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversation.getId());
        userMessage.setRole(AiRole.USER);
        userMessage.setContent(request.message());
        userMessage.setActionStatus(ActionStatus.NONE);
        messageMapper.insert(userMessage);

        Parsed parsed = intentParser.parse(request.message());

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversation.getId());
        assistantMessage.setRole(AiRole.ASSISTANT);
        assistantMessage.setContent(parsed.reply());
        assistantMessage.setPromptTokens(parsed.promptTokens());
        assistantMessage.setCompletionTokens(parsed.completionTokens());

        if (parsed.hasAction()) {
            assistantMessage.setIntent(parsed.intent());
            assistantMessage.setParsedPayload(parsed.payload().toString());
            assistantMessage.setActionStatus(ActionStatus.PENDING);
        } else {
            assistantMessage.setActionStatus(ActionStatus.NONE);
        }
        messageMapper.insert(assistantMessage);

        return new ChatResponse(conversation.getId(), toVO(userMessage), toVO(assistantMessage));
    }

    /** 用户确认：这才是唯一会写业务数据的入口 */
    @Transactional
    public AiMessageVO confirm(Long messageId) {
        AiMessage message = requireDraft(messageId);
        if (message.getActionStatus() == ActionStatus.EXECUTED) {
            return toVO(message);
        }
        if (message.getActionStatus() != ActionStatus.PENDING
                && message.getActionStatus() != ActionStatus.FAILED) {
            throw BusinessException.badRequest("这条草稿已经处理过了");
        }

        try {
            JsonNode payload = objectMapper.readTree(message.getParsedPayload());
            ExecutedAction result = actionExecutor.execute(message.getIntent(), payload);

            message.setActionStatus(ActionStatus.EXECUTED);
            message.setRelatedEntityType(result.entityType());
            message.setRelatedEntityId(result.entityId());
            message.setContent(message.getContent() + "\n\n✅ " + result.summary());
            messageMapper.updateById(message);
            return toVO(message);
        } catch (BusinessException ex) {
            // 落库失败要把原因留给用户看，同时把状态标成 FAILED 允许改口重来
            message.setActionStatus(ActionStatus.FAILED);
            message.setContent(message.getContent() + "\n\n⚠️ 没能记录：" + ex.getMessage());
            messageMapper.updateById(message);
            return toVO(message);
        } catch (Exception ex) {
            message.setActionStatus(ActionStatus.FAILED);
            messageMapper.updateById(message);
            throw BusinessException.badRequest("草稿内容无法解析，请重新说一次");
        }
    }

    @Transactional
    public AiMessageVO reject(Long messageId) {
        AiMessage message = requireDraft(messageId);
        if (message.getActionStatus() == ActionStatus.PENDING) {
            message.setActionStatus(ActionStatus.REJECTED);
            messageMapper.updateById(message);
        }
        return toVO(message);
    }

    // ---------------- 内部 ----------------

    private AiConversation createConversation(String firstMessage) {
        AiConversation conversation = new AiConversation();
        String title = firstMessage.strip();
        conversation.setTitle(title.length() > TITLE_MAX ? title.substring(0, TITLE_MAX) : title);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private AiConversation requireConversation(Long id) {
        AiConversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            throw BusinessException.notFound("找不到这个会话");
        }
        return conversation;
    }

    private AiMessage requireDraft(Long id) {
        AiMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw BusinessException.notFound("找不到这条消息");
        }
        if (message.getIntent() == null || message.getParsedPayload() == null) {
            throw BusinessException.badRequest("这条消息里没有待确认的动作");
        }
        return message;
    }

    private AiMessageVO toVO(AiMessage message) {
        String preview = null;
        if (message.getIntent() != null && message.getParsedPayload() != null
                && message.getActionStatus() == ActionStatus.PENDING) {
            try {
                preview = actionExecutor.preview(message.getIntent(),
                        objectMapper.readTree(message.getParsedPayload()));
            } catch (Exception ignored) {
                preview = null;
            }
        }
        return new AiMessageVO(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getIntent(),
                message.getActionStatus(),
                preview,
                null,
                message.getCreatedAt());
    }
}
