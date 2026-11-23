package com.volunteer.controller;

import com.volunteer.common.Result;
import com.volunteer.service.OrganizerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizers")
public class OrganizerController {

    @Autowired
    private OrganizerService organizerService;

    /** 组织者公开主页（游客可访问） */
    @GetMapping("/{id}")
    public Result<?> profile(@PathVariable Long id) {
        return Result.success(organizerService.profile(id));
    }
}
