package com.travelagent.travelplanner.chat.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.infrastructure.security.AuthenticatedUser;
import com.travelagent.travelplanner.chat.application.ChatService;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 实时对话会话与消息接口。 */
@RestController
@RequestMapping("/api/v1/conversations")
@PreAuthorize("hasAuthority('chat.use')")
@Validated
@Tag(name = "实时对话", description = "旅行规划多轮对话、消息历史与流式回复")
public class ConversationController {

    private final ChatService chatService;

    /**
     * 创建控制器。
     *
     * @param chatService 对话服务
     */
    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 创建会话。
     *
     * @param request 可选标题
     * @param authentication 当前认证信息
     * @return 新会话
     */
    @PostMapping
    @Operation(summary = "创建对话会话")
    public ApiResponse<ConversationResponse> create(
            @Valid @RequestBody(required = false) ConversationCreateRequest request,
            Authentication authentication) {
        String title = request == null ? null : request.title();
        return ApiResponse.ok(chatService.createConversation(currentUser(authentication).id(), title));
    }

    /**
     * 分页查询当前用户的会话。
     *
     * @param page 页码
     * @param size 每页条数
     * @param authentication 当前认证信息
     * @return 会话分页
     */
    @GetMapping
    @Operation(summary = "查询对话会话列表")
    public ApiResponse<ConversationListResponse> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            Authentication authentication) {
        return ApiResponse.ok(chatService.listConversations(currentUser(authentication).id(), page, size));
    }

    /**
     * 查询会话详情。
     *
     * @param conversationId 会话主键
     * @param authentication 当前认证信息
     * @return 会话
     */
    @GetMapping("/{conversationId}")
    @Operation(summary = "查询对话会话详情")
    public ApiResponse<ConversationResponse> detail(
            @PathVariable long conversationId,
            Authentication authentication) {
        return ApiResponse.ok(chatService.getConversation(
                currentUser(authentication).id(), conversationId));
    }

    /**
     * 删除会话。
     *
     * @param conversationId 会话主键
     * @param authentication 当前认证信息
     * @return 空响应
     */
    @DeleteMapping("/{conversationId}")
    @PreAuthorize("hasAuthority('chat.delete')")
    @Operation(summary = "删除对话会话")
    public ApiResponse<Void> delete(
            @PathVariable long conversationId,
            Authentication authentication) {
        chatService.deleteConversation(currentUser(authentication).id(), conversationId);
        return ApiResponse.ok(null);
    }

    /**
     * 查询消息历史。
     *
     * @param conversationId 会话主键
     * @param beforeId 只取该消息之前的记录
     * @param limit 每页条数
     * @param authentication 当前认证信息
     * @return 消息分页
     */
    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "查询对话消息历史")
    public ApiResponse<ChatMessagePageResponse> messages(
            @PathVariable long conversationId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            Authentication authentication) {
        return ApiResponse.ok(chatService.listMessages(
                currentUser(authentication).id(), conversationId, beforeId, limit));
    }

    /**
     * 发送消息并创建助手回复占位，随后通过 SSE 订阅增量。
     *
     * @param conversationId 会话主键
     * @param request 消息内容
     * @param authentication 当前认证信息
     * @return 用户消息与助手占位
     */
    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "发送对话消息")
    public ApiResponse<ChatExchangeResponse> send(
            @PathVariable long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        return ApiResponse.ok(chatService.sendMessage(
                currentUser(authentication).id(), conversationId, request.content()));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }
}
