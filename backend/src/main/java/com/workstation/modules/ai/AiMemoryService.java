package com.workstation.modules.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.workstation.common.exception.BusinessException;
import com.workstation.modules.ai.dto.AiMemoryVO;
import com.workstation.modules.ai.entity.AiMemory;
import com.workstation.modules.ai.mapper.AiMemoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 画像记忆的读写。记忆是永久的——不像聊天记录按天流转，
 * 所以这里没有按时间清理的逻辑，只有用户主动删除。
 */
@Service
public class AiMemoryService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int CONTENT_MAX = 500;
    /** 拼进提示词时的条数上限，避免记忆无限增长把上下文撑爆 */
    private static final int PROMPT_LIMIT = 60;

    private final AiMemoryMapper memoryMapper;

    public AiMemoryService(AiMemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    public List<AiMemoryVO> list() {
        return memoryMapper.selectList(
                        Wrappers.lambdaQuery(AiMemory.class).orderByDesc(AiMemory::getId))
                .stream().map(AiMemoryService::toVO).toList();
    }

    @Transactional
    public AiMemoryVO add(String content, String category, String source) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw BusinessException.badRequest("记忆内容不能为空");
        }
        if (text.length() > CONTENT_MAX) {
            text = text.substring(0, CONTENT_MAX);
        }
        AiMemory memory = new AiMemory();
        memory.setContent(text);
        memory.setCategory(normalizeCategory(category));
        memory.setSource(source == null ? "MANUAL" : source);
        memoryMapper.insert(memory);
        return toVO(memory);
    }

    @Transactional
    public void delete(Long id) {
        if (memoryMapper.selectById(id) == null) {
            throw BusinessException.notFound("找不到这条记忆");
        }
        memoryMapper.deleteById(id);
    }

    /**
     * 拼成给模型看的画像文本。没有记忆时返回空串，
     * 调用方据此决定要不要把这一段放进提示词。
     */
    public String describeForPrompt() {
        List<AiMemory> memories = memoryMapper.selectList(
                Wrappers.lambdaQuery(AiMemory.class)
                        .orderByAsc(AiMemory::getCategory)
                        .orderByAsc(AiMemory::getId)
                        .last("LIMIT " + PROMPT_LIMIT));
        if (memories.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (AiMemory memory : memories) {
            builder.append("- ").append(memory.getContent()).append('\n');
        }
        return builder.toString();
    }

    private static String normalizeCategory(String raw) {
        if (raw == null) {
            return "OTHER";
        }
        String value = raw.trim().toUpperCase();
        return switch (value) {
            case "PROFILE", "GOAL", "PREFERENCE", "HABIT" -> value;
            default -> "OTHER";
        };
    }

    private static AiMemoryVO toVO(AiMemory memory) {
        return new AiMemoryVO(
                memory.getId(),
                memory.getContent(),
                memory.getCategory(),
                memory.getSource(),
                memory.getCreatedAt() == null ? null : memory.getCreatedAt().format(TIME));
    }
}
