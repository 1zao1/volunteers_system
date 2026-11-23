package com.volunteer.controller;

import com.volunteer.common.Result;
import com.volunteer.service.OrganizerReviewService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class OrganizerReviewController {

    @Autowired
    private OrganizerReviewService reviewService;

    /** 志愿者：待评价的活动 */
    @GetMapping("/pending")
    public Result<?> pending(HttpServletRequest req) {
        return Result.success(reviewService.pending((Long) req.getAttribute("userId")));
    }

    /** 志愿者：提交评价 */
    @PostMapping
    public Result<?> submit(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Object regId = body.get("registrationId");
        Object rating = body.get("rating");
        Object comment = body.get("comment");
        Long rid = regId == null ? null : Long.valueOf(String.valueOf(regId));
        Integer score = rating == null ? null : Integer.valueOf(String.valueOf(rating));
        return Result.success(reviewService.submit(rid, score,
                comment == null ? null : String.valueOf(comment), (Long) req.getAttribute("userId")));
    }

    /** 组织者：收到的评价 */
    @GetMapping("/received")
    public Result<?> received(HttpServletRequest req) {
        return Result.success(reviewService.received((Long) req.getAttribute("userId")));
    }
}
