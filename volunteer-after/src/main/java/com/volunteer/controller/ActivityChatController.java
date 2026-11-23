package com.volunteer.controller;

import com.volunteer.common.Result;
import com.volunteer.service.ActivityChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/group-chat")
public class ActivityChatController {

    @Autowired
    private ActivityChatService activityChatService;

    private Long uid(HttpServletRequest req) {
        return (Long) req.getAttribute("userId");
    }

    @GetMapping("/list")
    public Result<?> list(HttpServletRequest req) {
        return Result.success(activityChatService.myGroups(uid(req),
                String.valueOf(req.getAttribute("role"))));
    }

    @GetMapping("/{activityId}/info")
    public Result<?> info(@PathVariable Long activityId, HttpServletRequest req) {
        return Result.success(activityChatService.info(activityId, uid(req)));
    }

    @GetMapping("/{activityId}/messages")
    public Result<?> messages(@PathVariable Long activityId, HttpServletRequest req) {
        return Result.success(activityChatService.messages(activityId, uid(req)));
    }

    @PostMapping("/{activityId}/messages")
    public Result<?> send(@PathVariable Long activityId, @RequestBody Map<String, Object> body,
                          HttpServletRequest req) {
        Object content = body.get("content");
        return Result.success(activityChatService.send(activityId, uid(req),
                content == null ? null : String.valueOf(content)));
    }
}
