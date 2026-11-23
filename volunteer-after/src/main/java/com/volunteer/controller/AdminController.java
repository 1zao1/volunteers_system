package com.volunteer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.common.Result;
import com.volunteer.dto.AttendanceDTO;
import com.volunteer.dto.AuditDTO;
import com.volunteer.entity.Activity;
import com.volunteer.entity.Registration;
import com.volunteer.entity.User;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.mapper.RegistrationMapper;
import com.volunteer.mapper.UserMapper;
import com.volunteer.service.ActivityService;
import com.volunteer.service.AttendanceService;
import com.volunteer.service.RegistrationService;
import com.volunteer.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private ActivityMapper activityMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private RegistrationMapper regMapper;
    @Autowired private ActivityService activityService;
    @Autowired private RegistrationService registrationService;
    @Autowired private AttendanceService attendanceService;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /* ================= 看板 ================= */
    @GetMapping("/dashboard")
    public Result<?> dashboard(HttpServletRequest req) {
        Long uid = (Long) req.getAttribute("userId");
        String role = String.valueOf(req.getAttribute("role"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        boolean isOrg = "organizer".equals(role);
        LambdaQueryWrapper<Activity> actW = new LambdaQueryWrapper<>();
        if (isOrg) actW.eq(Activity::getOrganizerId, uid);
        m.put("totalActivities", activityMapper.selectCount(actW));
        LambdaQueryWrapper<Activity> pubW = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, "published")
                .gt(Activity::getEndTime, LocalDateTime.now());
        if (isOrg) pubW.eq(Activity::getOrganizerId, uid);
        m.put("publishedActivities", activityMapper.selectCount(pubW));
        Set<Long> myAct = new HashSet<>();
        for (Activity a : activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOrganizerId, uid))) {
            myAct.add(a.getId());
        }
        m.put("totalVolunteers", myAct.isEmpty() ? 0 : distinctVolunteers(myAct));
        m.put("organizers", userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, "organizer")));
        m.put("totalHours", attendanceService.totalHours(uid));
        m.put("totalSignups", myAct.isEmpty() ? 0
                : regMapper.selectCount(new LambdaQueryWrapper<Registration>()
                        .in(Registration::getActivityId, myAct)));

        // 组织者看到的是自己活动范围内的待办
        List<Map<String, Object>> pendingList = registrationService.listForAdmin("pending", uid, role);
        long pendingHours = registrationService.listForAdmin("approved", uid, role).stream()
                .filter(r -> !(Boolean) r.get("issued") && Boolean.TRUE.equals(r.get("activityEnded")))
                .count();
        m.put("pendingAudit", pendingList.size());
        m.put("pendingHours", pendingHours);
        m.put("pendingUsers", pendingList.stream().map(r -> r.get("username")).distinct().limit(5).toList());

        m.put("categoryStats", categoryStats(uid, isOrg));
        m.put("trend", last7DaysTrend(myAct));
        m.put("recent", recentRegistrations(uid, isOrg));
        return Result.success(m);
    }

    private List<Map<String, Object>> categoryStats(Long uid, boolean onlyMine) {
        LambdaQueryWrapper<Activity> w = new LambdaQueryWrapper<>();
        if (onlyMine) w.eq(Activity::getOrganizerId, uid);
        List<Activity> all = activityMapper.selectList(w);
        Map<String, Long> count = new LinkedHashMap<>();
        for (Activity a : all) {
            count.merge(a.getCategory() == null ? "未分类" : a.getCategory(), 1L, Long::sum);
        }
        List<Map<String, Object>> res = new ArrayList<>();
        count.entrySet().stream()
                .sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    res.add(m);
                });
        return res;
    }

    private long distinctVolunteers(Set<Long> myAct) {
        List<Registration> regs = regMapper.selectList(new LambdaQueryWrapper<Registration>()
                .in(Registration::getActivityId, myAct)
                .ne(Registration::getStatus, "rejected"));
        return regs.stream().map(Registration::getUserId).distinct().count();
    }

    private List<Map<String, Object>> last7DaysTrend(Set<Long> myAct) {
        List<Map<String, Object>> res = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LambdaQueryWrapper<Registration> w = new LambdaQueryWrapper<Registration>()
                    .ge(Registration::getSignupTime, day.atStartOfDay())
                    .lt(Registration::getSignupTime, day.plusDays(1).atStartOfDay());
            if (!myAct.isEmpty()) w.in(Registration::getActivityId, myAct);
            Long c = regMapper.selectCount(w);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day.format(DAY));
            m.put("count", c == null ? 0 : c);
            res.add(m);
        }
        return res;
    }

    private List<Map<String, Object>> recentRegistrations(Long uid, boolean onlyMine) {
        List<Map<String, Object>> res = new ArrayList<>();
        List<Registration> list;
        if (onlyMine) {
            Set<Long> myAct = new HashSet<>();
            for (Activity a : activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                    .eq(Activity::getOrganizerId, uid))) {
                myAct.add(a.getId());
            }
            if (myAct.isEmpty()) return res;
            list = regMapper.selectList(new LambdaQueryWrapper<Registration>()
                    .in(Registration::getActivityId, myAct)
                    .orderByDesc(Registration::getSignupTime)
                    .last("LIMIT 6"));
        } else {
            list = regMapper.selectList(new LambdaQueryWrapper<Registration>()
                    .orderByDesc(Registration::getSignupTime)
                    .last("LIMIT 6"));
        }
        for (Registration r : list) {
            User u = userMapper.selectById(r.getUserId());
            Activity a = activityMapper.selectById(r.getActivityId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("username", u != null ? u.getUsername() : "已注销用户");
            m.put("activityTitle", a != null ? a.getTitle() : "活动已删除");
            m.put("status", r.getStatus());
            m.put("signupTime", r.getSignupTime() == null ? null : r.getSignupTime().format(FMT));
            res.add(m);
        }
        return res;
    }

    /* ================= 活动管理 ================= */
    @GetMapping("/activities")
    public Result<?> activities(HttpServletRequest req) {
        return Result.success(activityService.listAll((Long) req.getAttribute("userId")));
    }

    @PostMapping("/activities")
    public Result<?> createActivity(@RequestBody Activity a, HttpServletRequest req) {
        activityService.create(a, (Long) req.getAttribute("userId"));
        return Result.success();
    }

    @PutMapping("/activities/{id}")
    public Result<?> updateActivity(@PathVariable Long id, @RequestBody Activity a, HttpServletRequest req) {
        a.setId(id);
        activityService.update(a, (Long) req.getAttribute("userId"));
        return Result.success();
    }

    @PatchMapping("/activities/{id}/status")
    public Result<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                  HttpServletRequest req) {
        Object status = body.get("status");
        if (status == null) throw new RuntimeException("缺少 status 参数");
        activityService.changeStatus(id, String.valueOf(status), (Long) req.getAttribute("userId"));
        return Result.success();
    }

    @DeleteMapping("/activities/{id}")
    public Result<?> deleteActivity(@PathVariable Long id, HttpServletRequest req) {
        activityService.delete(id, (Long) req.getAttribute("userId"));
        return Result.success();
    }

    /* ================= 报名审核 / 工时 ================= */
    @GetMapping("/registrations")
    public Result<?> registrations(@RequestParam(defaultValue = "all") String status,
                                   HttpServletRequest req) {
        Long uid = (Long) req.getAttribute("userId");
        String role = String.valueOf(req.getAttribute("role"));
        return Result.success(registrationService.listForAdmin(status, uid, role));
    }

    @PostMapping("/registrations/{id}/audit")
    public Result<?> audit(@PathVariable Long id, @RequestBody AuditDTO dto, HttpServletRequest req) {
        registrationService.audit(id, dto.getStatus(),
                (Long) req.getAttribute("userId"), String.valueOf(req.getAttribute("role")));
        return Result.success();
    }

    @PostMapping("/registrations/{id}/absent")
    public Result<?> absent(@PathVariable Long id, HttpServletRequest req) {
        registrationService.markAbsent(id,
                (Long) req.getAttribute("userId"), String.valueOf(req.getAttribute("role")));
        return Result.success();
    }

    @PostMapping("/attendance")
    public Result<?> issueHours(@RequestBody AttendanceDTO dto, HttpServletRequest req) {
        if (dto.getRegistrationId() == null) throw new RuntimeException("缺少报名记录 id");
        attendanceService.issue(dto.getRegistrationId(), dto.getHours(), dto.getCreditScore(),
                (Long) req.getAttribute("userId"), String.valueOf(req.getAttribute("role")));
        return Result.success();
    }
}
