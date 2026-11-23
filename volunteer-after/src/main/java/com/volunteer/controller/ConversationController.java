package com.volunteer.controller;

import com.volunteer.common.Result;
import com.volunteer.service.ActivityChatService;
import com.volunteer.service.ConversationService;
import com.volunteer.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ActivityChatService activityChatService;

    private Long uid(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    @PostMapping("/pin")
    public Result<?> pin(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String type = String.valueOf(body.get("type"));
        Long targetId = Long.valueOf(String.valueOf(body.get("targetId")));
        boolean pinned = Boolean.parseBoolean(String.valueOf(body.get("pinned")));
        conversationService.setPinned(uid(req), type, targetId, pinned);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        String type = String.valueOf(body.get("type"));
        Long targetId = Long.valueOf(String.valueOf(body.get("targetId")));
        Long uid = uid(req);
        conversationService.delete(uid, type, targetId);
        // 删除会话同时清掉未读，避免列表里已经没有会话、导航栏还挂红点
        if ("dm".equals(type)) {
            messageService.markReadWith(uid, targetId);
        } else if ("group".equals(type)) {
            activityChatService.markReadToEnd(targetId, uid);
        }
        return Result.success();
    }
}
