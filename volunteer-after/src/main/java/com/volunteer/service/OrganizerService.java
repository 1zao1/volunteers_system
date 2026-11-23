package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.entity.Activity;
import com.volunteer.entity.Attendance;
import com.volunteer.entity.Registration;
import com.volunteer.entity.User;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.mapper.AttendanceMapper;
import com.volunteer.mapper.RegistrationMapper;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/** 组织者公开主页数据：游客也能查看，不返回手机号等隐私字段 */
@Service
public class OrganizerService {

    @Autowired private UserMapper userMapper;
    @Autowired private ActivityMapper activityMapper;
    @Autowired private RegistrationMapper regMapper;
    @Autowired private AttendanceMapper attendanceMapper;
    @Autowired private OrganizerReviewService reviewService;
    @Autowired private UserService userService;

    public Map<String, Object> profile(Long organizerId) {
        User u = userMapper.selectById(organizerId);
        if (u == null) throw new RuntimeException("发布者不存在");
        // 历史账号可能是 admin 角色但发布过活动，这里按发布者身份展示

        List<Activity> acts = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOrganizerId, organizerId)
                .orderByDesc(Activity::getStartTime));
        Set<Long> actIds = new HashSet<>();
        Set<Long> volunteers = new HashSet<>();
        int ended = 0;
        List<Activity> upcoming = new ArrayList<>();
        for (Activity a : acts) {
            actIds.add(a.getId());
            if (a.getEndTime() != null && a.getEndTime().isBefore(LocalDateTime.now())) ended++;
            else if ("published".equals(a.getStatus())) upcoming.add(a);
        }
        if (!actIds.isEmpty()) {
            for (Registration r : regMapper.selectList(new LambdaQueryWrapper<Registration>()
                    .in(Registration::getActivityId, actIds)
                    .eq(Registration::getStatus, "approved"))) {
                if (r.getUserId() != null) volunteers.add(r.getUserId());
            }
        }
        int hours = 0;
        if (!actIds.isEmpty()) {
            for (Attendance t : attendanceMapper.selectList(new LambdaQueryWrapper<Attendance>()
                    .in(Attendance::getActivityId, actIds))) {
                hours += t.getHours() == null ? 0 : t.getHours();
            }
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nickname", StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        m.put("username", u.getUsername());
        m.put("avatar", u.getAvatar());
        m.put("signature", u.getSignature());
        m.put("creditScore", u.getCreditScore());
        m.put("creditDesc", userService.creditDesc(u.getCreditScore()));
        m.put("joinedAt", u.getCreateTime());
        m.put("activityCount", acts.size());
        m.put("endedCount", ended);
        m.put("upcomingCount", upcoming.size());
        m.put("volunteerCount", volunteers.size());
        m.put("totalHours", hours);
        m.putAll(reviewService.summary(organizerId));

        List<Map<String, Object>> cards = new ArrayList<>();
        for (Activity a : upcoming.subList(0, Math.min(4, upcoming.size()))) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", a.getId());
            c.put("title", a.getTitle());
            c.put("category", a.getCategory());
            c.put("cover", a.getCover());
            c.put("location", a.getLocation());
            c.put("startTime", a.getStartTime());
            c.put("endTime", a.getEndTime());
            c.put("hours", a.getHours());
            c.put("signedCount", a.getSignedCount());
            c.put("maxCount", a.getMaxCount());
            cards.add(c);
        }
        m.put("activities", cards);
        m.put("reviews", reviewService.received(organizerId).get("reviews"));
        return m;
    }
}
