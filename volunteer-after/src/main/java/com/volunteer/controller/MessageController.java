package com.volunteer.controller;

import com.volunteer.common.Result;
import com.volunteer.service.ActivityChatService;
import com.volunteer.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private ActivityChatService activityChatService;

    private Long uid(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    @GetMapping("/contacts")
    public Result<?> contacts(HttpServletRequest req) {
        return Result.success(messageService.contacts(uid(req)));
    }

    @GetMapping("/with/{otherId}")
    public Result<?> thread(@PathVariable Long otherId, HttpServletRequest req) {
        return Result.success(messageService.thread(uid(req), otherId));
    }

    @PostMapping
    public Result<?> send(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Object to = body.get("toUserId");
        if (to == null) throw new RuntimeException("请选择聊天对象");
        Long toId = Long.valueOf(String.valueOf(to));
        Object content = body.get("content");
        Long id = messageService.send(uid(req), toId, content == null ? null : String.valueOf(content));
        return Result.success(id);
    }

    @GetMapping("/unread")
    public Result<?> unread(HttpServletRequest req) {
        Long uid = uid(req);
        String role = String.valueOf(req.getAttribute("role"));
        // 导航栏红点 = 私聊未读 + 我参与的群聊未读
        return Result.success(messageService.unread(uid) + activityChatService.unreadTotal(uid, role));
    }
}
