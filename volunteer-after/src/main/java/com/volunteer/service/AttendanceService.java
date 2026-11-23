package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceMapper attMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private RegistrationMapper regMapper;
    @Autowired
    private UserMapper userMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 已经认定的工时 */
    public List<Map<String, Object>> myHours(Long userId) {
        List<Attendance> list = attMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getUserId, userId)
                .orderByDesc(Attendance::getSignTime)
                .orderByDesc(Attendance::getId));
        List<Map<String, Object>> res = new ArrayList<>();
        for (Attendance a : list) {
            Activity act = activityMapper.selectById(a.getActivityId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("activityId", a.getActivityId());
            m.put("activityTitle", act != null ? act.getTitle() : "活动已删除");
            m.put("category", act != null ? act.getCategory() : null);
            m.put("cover", act != null ? act.getCover() : null);
            m.put("location", act != null ? act.getLocation() : null);
            m.put("hours", a.getHours());
            m.put("date", a.getSignTime() == null ? null : a.getSignTime().format(FMT));
            res.add(m);
        }
        return res;
    }

    /** 审核已通过、活动已结束、但还没认定工时的记录 */
    public List<Map<String, Object>> pendingHours(Long userId) {
        return approvedWithoutHours(userId, true);
    }

    /** 审核已通过、活动尚未结束的报名，属于待参与，不是待认定 */
    public List<Map<String, Object>> upcomingHours(Long userId) {
        return approvedWithoutHours(userId, false);
    }

    private List<Map<String, Object>> approvedWithoutHours(Long userId, boolean ended) {
        Set<Long> issued = new HashSet<>();
        for (Attendance t : attMapper.selectList(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getUserId, userId))) {
            if (t.getRegistrationId() != null) issued.add(t.getRegistrationId());
        }
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> res = new ArrayList<>();
        for (Registration r : regMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getStatus, "approved"))) {
            if (issued.contains(r.getId())) continue;
            Activity a = activityMapper.selectById(r.getActivityId());
            if (a == null) continue;
            boolean isEnded = a.getEndTime() != null && a.getEndTime().isBefore(now);
            if (isEnded != ended) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("activityId", a.getId());
            m.put("activityTitle", a.getTitle());
            m.put("category", a.getCategory());
            m.put("cover", a.getCover());
            m.put("location", a.getLocation());
            m.put("hours", a.getHours());
            m.put("startTime", a.getStartTime() == null ? null : a.getStartTime().format(FMT));
            m.put("endTime", a.getEndTime() == null ? null : a.getEndTime().format(FMT));
            res.add(m);
        }
        return res;
    }

    @Transactional
    public void issue(Long registrationId, Integer hours, Integer creditScore, Long operatorId, String role) {
        if (!"organizer".equals(role)) {
            throw new RuntimeException("工时认定由活动组织者完成");
        }
        Registration r = regMapper.selectById(registrationId);
        if (r == null) throw new RuntimeException("报名记录不存在");
        if (!"approved".equals(r.getStatus())) throw new RuntimeException("只有审核通过的报名才能认定工时");
        Activity act = activityMapper.selectById(r.getActivityId());
        if (act == null) throw new RuntimeException("活动已删除");
        if (act.getOrganizerId() == null || !act.getOrganizerId().equals(operatorId)) {
            throw new RuntimeException("只能为自己发布的活动认定工时");
        }
        if (act.getEndTime() != null && act.getEndTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("活动还没结束，结束后再认定工时");
        }
        if (r.getUserId().equals(operatorId)) throw new RuntimeException("不能给自己认定工时");
        if (attMapper.selectCount(new LambdaQueryWrapper<Attendance>()
                .eq(Attendance::getRegistrationId, registrationId)) > 0) {
            throw new RuntimeException("该报名已经认定过工时");
        }
        int h = (hours != null && hours > 0) ? hours : (act.getHours() != null ? act.getHours() : 0);
        if (h <= 0) throw new RuntimeException("工时必须大于 0");

        Attendance t = new Attendance();
        t.setRegistrationId(registrationId);
        t.setUserId(r.getUserId());
        t.setActivityId(r.getActivityId());
        t.setHours(h);
        t.setSignTime(LocalDateTime.now());
        attMapper.insert(t);

        // 信誉分由组织者在认定工时时一并给出
        if (creditScore != null) {
            int c = Math.max(0, Math.min(100, creditScore));
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, r.getUserId())
                    .set(User::getCreditScore, c)
                    .set(User::getCreditTime, LocalDateTime.now()));
        }
    }

    /**
     * 历史报名补齐：活动已结束、报名已通过、但还没有工时记录 → 自动按活动工时补一条。
     * 这样"参加了活动却没有时长"的历史数据也能一次性补齐。
     */
    @Transactional
    public int autoIssueForFinished() {
        int count = 0;
        Set<Long> issued = new HashSet<>();
        for (Attendance t : attMapper.selectList(null)) {
            if (t.getRegistrationId() != null) issued.add(t.getRegistrationId());
        }
        for (Registration r : regMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getStatus, "approved"))) {
            if (issued.contains(r.getId())) continue;
            Activity a = activityMapper.selectById(r.getActivityId());
            if (a == null || a.getEndTime() == null || a.getEndTime().isAfter(LocalDateTime.now())) continue;
            if (a.getHours() == null || a.getHours() <= 0) continue;
            Attendance t = new Attendance();
            t.setRegistrationId(r.getId());
            t.setUserId(r.getUserId());
            t.setActivityId(r.getActivityId());
            t.setHours(a.getHours());
            t.setSignTime(a.getEndTime());
            attMapper.insert(t);
            count++;
        }
        return count;
    }

    public long totalHours() {
        return attMapper.selectList(null).stream()
                .mapToLong(a -> a.getHours() == null ? 0 : a.getHours())
                .sum();
    }

    /** 某个组织者名下活动累计认定的工时 */
    public long totalHours(Long organizerId) {
        List<Long> actIds = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                        .eq(Activity::getOrganizerId, organizerId))
                .stream().map(Activity::getId).toList();
        if (actIds.isEmpty()) return 0;
        return attMapper.selectList(new LambdaQueryWrapper<Attendance>()
                        .in(Attendance::getActivityId, actIds))
                .stream().mapToLong(a -> a.getHours() == null ? 0 : a.getHours()).sum();
    }
}
