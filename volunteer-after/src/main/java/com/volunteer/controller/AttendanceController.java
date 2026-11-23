package com.volunteer.controller;

import com.volunteer.common.Result;
import com.volunteer.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /** 已认定工时 + 待认定工时一次返回，避免"做了活动却没有时长"的疑惑 */
    @GetMapping("/my")
    public Result<?> my(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issued", attendanceService.myHours(userId));
        m.put("pending", attendanceService.pendingHours(userId));
        m.put("upcoming", attendanceService.upcomingHours(userId));
        return Result.success(m);
    }
}
