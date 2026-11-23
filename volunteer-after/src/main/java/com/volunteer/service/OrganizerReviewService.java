package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.entity.*;
import com.volunteer.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrganizerReviewService {

    /** 每累计 5 个差评（不同志愿者）扣 5 分，之后每再满 5 个再扣 5 分 */
    public static final int BAD_THRESHOLD = 5;
    public static final int BAD_DEDUCT = 5;

    @Autowired private OrganizerReviewMapper reviewMapper;
    @Autowired private RegistrationMapper regMapper;
    @Autowired private ActivityMapper activityMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private UserService userService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional
    public Map<String, Object> submit(Long registrationId, Integer rating, String comment, Long userId) {
        if (registrationId == null) throw new RuntimeException("缺少报名记录");
        if (rating == null || rating < 1 || rating > 5) throw new RuntimeException("评分请选择 1-5 星");
        Registration r = regMapper.selectById(registrationId);
        if (r == null) throw new RuntimeException("报名记录不存在");
        if (!r.getUserId().equals(userId)) throw new RuntimeException("只能评价自己参加的活动");
        if ("absent".equals(r.getStatus())) throw new RuntimeException("缺席记录不能评价组织者");
        if (!"approved".equals(r.getStatus())) throw new RuntimeException("报名审核通过后才能评价");
        Activity a = activityMapper.selectById(r.getActivityId());
        if (a == null) throw new RuntimeException("活动已删除");
        if (a.getEndTime() == null || a.getEndTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("活动结束后才能评价组织者");
        }
        if (reviewMapper.selectCount(new LambdaQueryWrapper<OrganizerReview>()
                .eq(OrganizerReview::getRegistrationId, registrationId)) > 0) {
            throw new RuntimeException("这条报名已经评价过了");
        }

        String c = comment == null ? null : comment.trim();
        if (c != null && c.length() > 120) c = c.substring(0, 120);

        OrganizerReview rev = new OrganizerReview();
        rev.setRegistrationId(registrationId);
        rev.setActivityId(r.getActivityId());
        rev.setVolunteerId(userId);
        rev.setOrganizerId(a.getOrganizerId());
        rev.setRating(rating);
        rev.setComment(StringUtils.hasText(c) ? c : null);
        rev.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(rev);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("rating", rating);
        boolean deducted = false;
        if (a.getOrganizerId() != null) {
            int bad = badVolunteerCount(a.getOrganizerId());
            res.put("badCount", bad);
            // 差评人数刚满 5 的整数倍时扣分，避免重复扣
            if (bad > 0 && bad % BAD_THRESHOLD == 0) {
                userService.deductCredit(a.getOrganizerId(), BAD_DEDUCT);
                deducted = true;
            }
        }
        res.put("deducted", deducted);
        res.put("message", deducted
                ? "评价已提交；该组织者差评人数达到 " + BAD_THRESHOLD + " 人，信誉分已扣 5 分"
                : "评价已提交，感谢你的反馈");
        return res;
    }

    /** 待评价：已通过、活动已结束、未评价、非缺席 */
    public List<Map<String, Object>> pending(Long userId) {
        List<Map<String, Object>> res = new ArrayList<>();
        Set<Long> reviewed = new HashSet<>();
        for (OrganizerReview rev : reviewMapper.selectList(new LambdaQueryWrapper<OrganizerReview>()
                .eq(OrganizerReview::getVolunteerId, userId))) {
            reviewed.add(rev.getRegistrationId());
        }
        for (Registration r : regMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getStatus, "approved"))) {
            if (reviewed.contains(r.getId())) continue;
            Activity a = activityMapper.selectById(r.getActivityId());
            if (a == null || a.getEndTime() == null || !a.getEndTime().isBefore(LocalDateTime.now())) continue;
            User o = a.getOrganizerId() == null ? null : userMapper.selectById(a.getOrganizerId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("registrationId", r.getId());
            m.put("activityId", a.getId());
            m.put("activityTitle", a.getTitle());
            m.put("category", a.getCategory());
            m.put("cover", a.getCover());
            m.put("endTime", a.getEndTime().format(FMT));
            m.put("organizerId", a.getOrganizerId());
            m.put("organizerName", o == null ? "发布者已注销" : displayName(o));
            res.add(m);
        }
        return res;
    }

    /** 组织者收到的评价与汇总 */
    public Map<String, Object> received(Long organizerId) {
        List<OrganizerReview> list = reviewMapper.selectList(new LambdaQueryWrapper<OrganizerReview>()
                .eq(OrganizerReview::getOrganizerId, organizerId)
                .orderByDesc(OrganizerReview::getCreateTime)
                .orderByDesc(OrganizerReview::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        int total = 0;
        int bad = 0;
        for (OrganizerReview rev : list) {
            User v = userMapper.selectById(rev.getVolunteerId());
            Activity a = activityMapper.selectById(rev.getActivityId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rev.getId());
            m.put("volunteerName", v == null ? "志愿者" : displayName(v));
            m.put("activityTitle", a == null ? "活动已删除" : a.getTitle());
            m.put("rating", rev.getRating());
            m.put("comment", rev.getComment());
            m.put("time", rev.getCreateTime() == null ? null : rev.getCreateTime().format(FMT));
            rows.add(m);
            total += rev.getRating() == null ? 0 : rev.getRating();
            if (isBad(rev.getRating())) bad++;
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("count", list.size());
        res.put("avgRating", list.isEmpty() ? null : Math.round((total * 10.0 / list.size())) / 10.0);
        res.put("badCount", bad);
        res.put("reviews", rows);
        return res;
    }

    public Map<String, Object> summary(Long organizerId) {
        List<OrganizerReview> list = reviewMapper.selectList(new LambdaQueryWrapper<OrganizerReview>()
                .eq(OrganizerReview::getOrganizerId, organizerId));
        int total = 0;
        int bad = 0;
        for (OrganizerReview rev : list) {
            total += rev.getRating() == null ? 0 : rev.getRating();
            if (isBad(rev.getRating())) bad++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reviewCount", list.size());
        m.put("avgRating", list.isEmpty() ? null : Math.round((total * 10.0 / list.size())) / 10.0);
        m.put("badCount", bad);
        return m;
    }

    public boolean prepared(Long registrationId) {
        return reviewMapper.selectCount(new LambdaQueryWrapper<OrganizerReview>()
                .eq(OrganizerReview::getRegistrationId, registrationId)) > 0;
    }

    private int badVolunteerCount(Long organizerId) {
        Set<Long> people = new HashSet<>();
        for (OrganizerReview rev : reviewMapper.selectList(new LambdaQueryWrapper<OrganizerReview>()
                .eq(OrganizerReview::getOrganizerId, organizerId))) {
            if (isBad(rev.getRating())) people.add(rev.getVolunteerId());
        }
        return people.size();
    }

    private boolean isBad(Integer rating) {
        return rating != null && rating <= 2;
    }

    private String displayName(User u) {
        return StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername();
    }
}
