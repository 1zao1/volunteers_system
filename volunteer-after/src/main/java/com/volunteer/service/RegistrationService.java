package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.volunteer.entity.Activity;
import com.volunteer.entity.Attendance;
import com.volunteer.entity.OrganizerReview;
import com.volunteer.entity.Registration;
import com.volunteer.entity.User;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.mapper.AttendanceMapper;
import com.volunteer.mapper.RegistrationMapper;
import com.volunteer.mapper.OrganizerReviewMapper;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RegistrationService {

    @Autowired
    private RegistrationMapper regMapper;
    @Autowired
    private OrganizerReviewMapper reviewMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AttendanceMapper attendanceMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private ActivityChatService activityChatService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String fmt(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }

    /**
     * 报名。规则：
     * 1) 只有 published 且未结束、未过报名截止时间的活动可报；
     * 2) 不能报名自己发布的活动（避免"自己审核自己"）；
     * 3) 信誉分低于 85 视为低信誉（组织者同样适用），仍可报名，提示由前端给出；
     * 4) 名额用条件更新，避免并发超额。
     */
    @Transactional
    public Long signup(Long userId, Long activityId, String role) {
        Activity a = activityMapper.selectById(activityId);
        if (a == null) throw new RuntimeException("活动不存在或已下架");
        if (!"published".equals(a.getStatus())) throw new RuntimeException("该活动暂未开放报名");
        LocalDateTime now = LocalDateTime.now();
        if (a.getEndTime() != null && a.getEndTime().isBefore(now)) {
            throw new RuntimeException("该活动已经结束");
        }
        if (a.getSignupDeadline() != null && now.isAfter(a.getSignupDeadline())) {
            throw new RuntimeException("报名已于 " + fmt(a.getSignupDeadline()) + " 截止");
        }
        if ("organizer".equals(role) && a.getOrganizerId() != null && a.getOrganizerId().equals(userId)) {
            throw new RuntimeException("不能报名自己发布的活动，请到「报名审核」处理其他人");
        }
        User u = userMapper.selectById(userId);
        // 信誉分低于 85 仍可报名（提示由前端给出），只是会被标记为低信誉
        int credit = u == null || u.getCreditScore() == null ? 100 : u.getCreditScore();
        if (credit < 0) {
            throw new RuntimeException("信誉分异常，请联系活动组织者");
        }
        if (regMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId)
                .ne(Registration::getStatus, "absent")) > 0) {
            throw new RuntimeException("你已经报名过该活动了");
        }
        int updated = activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .apply("signed_count < max_count")
                .setSql("signed_count = signed_count + 1"));
        if (updated == 0) throw new RuntimeException("名额已满，看看其他活动吧");

        Registration r = new Registration();
        r.setUserId(userId);
        r.setActivityId(activityId);
        r.setStatus("pending");
        regMapper.insert(r);
        return r.getId();
    }

    @Transactional
    public void cancel(Long userId, Long regId) {
        Registration r = regMapper.selectById(regId);
        if (r == null || !r.getUserId().equals(userId)) throw new RuntimeException("报名记录不存在");
        if (!"pending".equals(r.getStatus())) throw new RuntimeException("已审核的报名请联系活动组织者处理");
        regMapper.deleteById(regId);
        activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, r.getActivityId())
                .apply("signed_count > 0")
                .setSql("signed_count = signed_count - 1"));
    }

    public List<Map<String, Object>> myList(Long userId) {
        List<Registration> list = regMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .orderByDesc(Registration::getSignupTime)
                .orderByDesc(Registration::getId));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Registration r : list) {
            Activity a = activityMapper.selectById(r.getActivityId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("activityId", r.getActivityId());
            m.put("activityTitle", a != null ? a.getTitle() : "活动已删除");
            m.put("category", a != null ? a.getCategory() : null);
            m.put("cover", a != null ? a.getCover() : null);
            m.put("location", a != null ? a.getLocation() : null);
            m.put("startTime", a != null ? fmt(a.getStartTime()) : null);
            m.put("endTime", a != null ? fmt(a.getEndTime()) : null);
            m.put("hours", a != null ? a.getHours() : null);
            m.put("status", r.getStatus());
            m.put("signupTime", fmt(r.getSignupTime()));
            m.put("organizerId", a != null ? a.getOrganizerId() : null);
            if (a != null && a.getOrganizerId() != null) {
                User o = userMapper.selectById(a.getOrganizerId());
                m.put("organizerName", o == null ? "发布者已注销"
                        : (o.getNickname() != null && !o.getNickname().isBlank() ? o.getNickname() : o.getUsername()));
            }
            m.put("ended", a != null && a.getEndTime() != null
                    && a.getEndTime().isBefore(java.time.LocalDateTime.now()));
            m.put("reviewed", reviewMapper.selectCount(new LambdaQueryWrapper<OrganizerReview>()
                    .eq(OrganizerReview::getRegistrationId, r.getId())) > 0);
            res.add(m);
        }
        return res;
    }

    /**
     * 管理端报名列表。
     * organizer 只能看到自己发布活动的报名；admin 只读全站。
     */
    public List<Map<String, Object>> listForAdmin(String status, Long operatorId, String role) {
        LambdaQueryWrapper<Registration> w = new LambdaQueryWrapper<Registration>()
                .orderByDesc(Registration::getSignupTime)
                .orderByDesc(Registration::getId);
        if (status != null && !"all".equals(status) && !status.isEmpty()) {
            w.eq(Registration::getStatus, status);
        }
        List<Registration> list = regMapper.selectList(w);

        Set<Long> issuedIds = new HashSet<>();
        for (Attendance t : attendanceMapper.selectList(null)) {
            if (t.getRegistrationId() != null) issuedIds.add(t.getRegistrationId());
        }
        boolean isOrganizer = "organizer".equals(role);

        List<Map<String, Object>> res = new ArrayList<>();
        for (Registration r : list) {
            Activity a = activityMapper.selectById(r.getActivityId());
            // 组织者视角：只保留自己发布的活动
            if (isOrganizer && (a == null || a.getOrganizerId() == null
                    || !a.getOrganizerId().equals(operatorId))) {
                continue;
            }
            User u = userMapper.selectById(r.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            m.put("username", u != null ? displayName(u) : "用户已注销");
            m.put("nickname", u != null ? displayName(u) : "用户已注销");
            m.put("phone", u != null ? u.getPhone() : null);
            m.put("skills", u != null ? u.getSkills() : null);
            m.put("creditScore", u != null ? u.getCreditScore() : null);
            m.put("activityId", r.getActivityId());
            m.put("activityTitle", a != null ? a.getTitle() : "活动已删除");
            m.put("organizerId", a != null ? a.getOrganizerId() : null);
            m.put("category", a != null ? a.getCategory() : null);
            m.put("hours", a != null ? a.getHours() : null);
            m.put("startTime", a != null ? fmt(a.getStartTime()) : null);
            m.put("endTime", a != null ? fmt(a.getEndTime()) : null);
            m.put("activityEnded", a != null && a.getEndTime() != null
                    && a.getEndTime().isBefore(LocalDateTime.now()));
            m.put("status", r.getStatus());
            m.put("signupTime", fmt(r.getSignupTime()));
            m.put("issued", issuedIds.contains(r.getId()));
            boolean owned = a != null && a.getOrganizerId() != null && a.getOrganizerId().equals(operatorId);
            m.put("isSelf", r.getUserId().equals(operatorId));
            m.put("canAudit", isOrganizer && owned && !r.getUserId().equals(operatorId));
            res.add(m);
        }
        return res;
    }

    private String displayName(User u) {
        return u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername();
    }

    /** 校验：当前操作人必须是该活动的组织者，且报名人不能是自己 */
    private Activity requireOwner(Long regId, Long operatorId, String role) {
        if (!"organizer".equals(role)) {
            throw new RuntimeException("报名审核由活动组织者负责");
        }
        Registration r = regMapper.selectById(regId);
        if (r == null) throw new RuntimeException("报名记录不存在");
        Activity a = activityMapper.selectById(r.getActivityId());
        if (a == null) throw new RuntimeException("活动已删除");
        if (a.getOrganizerId() == null || !a.getOrganizerId().equals(operatorId)) {
            throw new RuntimeException("只能处理自己发布的活动报名");
        }
        if (r.getUserId().equals(operatorId)) {
            throw new RuntimeException("不能审核自己的报名");
        }
        return a;
    }

    @Transactional
    public void audit(Long regId, String status, Long operatorId, String role) {
        if (!"approved".equals(status) && !"rejected".equals(status)) {
            throw new RuntimeException("审核状态只能是 approved 或 rejected");
        }
        requireOwner(regId, operatorId, role);
        Registration r = regMapper.selectById(regId);
        if (!"pending".equals(r.getStatus())) throw new RuntimeException("该报名已处理过");

        r.setStatus(status);
        regMapper.updateById(r);

        if ("rejected".equals(status)) {
            activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, r.getActivityId())
                    .apply("signed_count > 0")
                    .setSql("signed_count = signed_count - 1"));
        } else {
            // 审核通过即入群，把入群前的历史消息标为已读
            activityChatService.markReadToEnd(r.getActivityId(), r.getUserId());
        }
    }

    /** 标记缺席：删除已发放工时、扣 5 分信誉、报名状态置 absent */
    @Transactional
    public void markAbsent(Long regId, Long operatorId, String role) {
        requireOwner(regId, operatorId, role);
        Registration r = regMapper.selectById(regId);
        if ("rejected".equals(r.getStatus())) throw new RuntimeException("已驳回的报名无需标记缺席");
        if ("absent".equals(r.getStatus())) throw new RuntimeException("该报名已标记为缺席");

        regMapper.update(null, new LambdaUpdateWrapper<Registration>()
                .eq(Registration::getId, regId)
                .set(Registration::getStatus, "absent"));
        attendanceMapper.delete(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getRegistrationId, regId));
        userService.deductCredit(r.getUserId(), 5);
    }
}
