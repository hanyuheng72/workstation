package com.workstation.modules.ai;

import com.workstation.common.result.ApiResponse;
import com.workstation.modules.ai.dto.AiMessageVO;
import com.workstation.modules.ai.dto.AiStatusVO;
import com.workstation.modules.ai.dto.AiSummaryVO;
import com.workstation.modules.ai.dto.AnalysisVO;
import com.workstation.modules.ai.dto.ChatRequest;
import com.workstation.modules.ai.dto.ChatResponse;
import com.workstation.modules.ai.dto.ConversationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService chatService;
    private final AiSummaryService summaryService;

    public AiController(AiChatService chatService, AiSummaryService summaryService) {
        this.chatService = chatService;
        this.summaryService = summaryService;
    }

    /** 只回报配没配 Key，绝不回传 Key 本身 */
    @GetMapping("/status")
    public ApiResponse<AiStatusVO> status() {
        return ApiResponse.ok(new AiStatusVO(summaryService.isConfigured(), summaryService.model()));
    }

    /** 只读缓存，不会触发模型调用 */
    @GetMapping("/summary/today")
    public ApiResponse<AiSummaryVO> todaySummary() {
        return ApiResponse.ok(summaryService.today());
    }

    /** 生成或刷新今日总结，会真实调用模型 */
    @PostMapping("/summary/today")
    public ApiResponse<AiSummaryVO> generateTodaySummary() {
        return ApiResponse.ok(summaryService.generateToday());
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(chatService.chat(request));
    }

    /** 用户确认后才真正写业务数据 */
    @PostMapping("/actions/{messageId}/confirm")
    public ApiResponse<AiMessageVO> confirm(@PathVariable Long messageId) {
        return ApiResponse.ok(chatService.confirm(messageId));
    }

    @PostMapping("/actions/{messageId}/reject")
    public ApiResponse<AiMessageVO> reject(@PathVariable Long messageId) {
        return ApiResponse.ok(chatService.reject(messageId));
    }

    @GetMapping("/analysis/expense")
    public ApiResponse<AnalysisVO> analyzeExpense(@RequestParam(required = false) String month) {
        return ApiResponse.ok(new AnalysisVO(summaryService.analyzeExpense(month)));
    }

    @GetMapping("/analysis/workout")
    public ApiResponse<AnalysisVO> analyzeWorkout() {
        return ApiResponse.ok(new AnalysisVO(summaryService.analyzeWorkout()));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationVO>> conversations() {
        return ApiResponse.ok(chatService.listConversations());
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<AiMessageVO>> messages(@PathVariable Long id) {
        return ApiResponse.ok(chatService.listMessages(id));
    }
}
