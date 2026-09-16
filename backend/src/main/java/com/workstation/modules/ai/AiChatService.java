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
import com.workstation.modules.dashboard.DashboardService;
import com.workstation.modules.finance.FinanceService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话与「解析 → 草稿 → 确认 → 落库」这条链路。
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int TITLE_MAX = 30;
    /** 带进提示词的最近几条对话，太多既费钱也没必要 */
    private static final int HISTORY_LIMIT = 10;

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final IntentParser intentParser;
    private final ActionExecutor actionExecutor;
    private final AiMemoryService memoryService;
    private final DashboardService dashboardService;
    private final FinanceService financeService;
    private final ObjectMapper objectMapper;

    public AiChatService(AiConversationMapper conversationMapper, AiMessageMapper messageMapper,
                         IntentParser intentParser, ActionExecutor actionExecutor,
                         AiMemoryService memoryService, DashboardService dashboardService,
                         FinanceService financeService, ObjectMapper objectMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.intentParser = intentParser;
        this.actionExecutor = actionExecutor;
        this.memoryService = memoryService;
        this.dashboardService = dashboardService;
        this.financeService = financeService;
        this.objectMapper = objectMapper;
    }

    /** 今天的对话。记忆是永久的，但聊天上下文按天重置，不让昨天的话题拖到今天 */
    public List<AiMessageVO> todayMessages() {
        return messageMapper.selectList(
                        Wrappers.lambdaQuery(AiMessage.class)
                                .ge(AiMessage::getCreatedAt, LocalDate.now().atStartOfDay())
                                .orderByAsc(AiMessage::getId))
                .stream().map(this::toVO).toList();
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

        Parsed parsed = intentParser.parse(
                request.message(), todayHistory(), memoryService.describeForPrompt(), dataSnapshot());

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

    /**
     * 今天已发生的对话，最近的排在后面。只取今天的：
     * 记忆是长期的，但每天的闲聊不该拖成一条无限长的上下文。
     */
    private List<ChatMessage> todayHistory() {
        List<AiMessage> recent = messageMapper.selectList(
                Wrappers.lambdaQuery(AiMessage.class)
                        .ge(AiMessage::getCreatedAt, LocalDate.now().atStartOfDay())
                        .orderByDesc(AiMessage::getId)
                        .last("LIMIT " + HISTORY_LIMIT));

        List<ChatMessage> history = new ArrayList<>();
        for (int index = recent.size() - 1; index >= 0; index--) {
            AiMessage message = recent.get(index);
            if (message.getRole() == AiRole.USER) {
                history.add(ChatMessage.user(message.getContent()));
            } else if (message.getRole() == AiRole.ASSISTANT) {
                history.add(ChatMessage.assistant(message.getContent()));
            }
        }
        return history;
    }

    /**
     * 给助手看的数据上下文。
     *
     * 除了今天的快照，还带上上个月的收支——不然用户问「比上个月多花多少」时，
     * 助手只能诚实地说看不到（实测就是这样）。
     */
    private String dataSnapshot() {
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("today", dashboardService.overview());
            context.put("lastMonth", financeService.monthAmounts(YearMonth.now().minusMonths(1)));
            return objectMapper.writeValueAsString(context);
        } catch (Exception ex) {
            log.warn("生成数据快照失败: {}", ex.getMessage());
            return "（暂时取不到数据）";
        }
    }

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
