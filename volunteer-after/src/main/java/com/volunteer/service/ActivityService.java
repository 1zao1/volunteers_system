package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.entity.Activity;
import com.volunteer.entity.User;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityService {

    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private UserMapper userMapper;

    /** 活动广场：仅展示已发布且尚未结束的活动，按开始时间正序 */
    public List<Activity> list(String category, String keyword) {
        LambdaQueryWrapper<Activity> w = new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, "published")
                .gt(Activity::getEndTime, LocalDateTime.now())
                .orderByAsc(Activity::getStartTime);
        if (StringUtils.hasText(category) && !"全部".equals(category)) {
            w.eq(Activity::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(Activity::getTitle, keyword).or().like(Activity::getLocation, keyword));
        }
        List<Activity> list = activityMapper.selectList(w);
        fillOrganizer(list);
        return list;
    }

    /** 管理端：只列出当前组织者自己发布的活动（含草稿与已结束） */
    public List<Activity> listAll(Long organizerId) {
        List<Activity> list = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getOrganizerId, organizerId)
                .orderByDesc(Activity::getCreateTime)
                .orderByDesc(Activity::getId));
        fillOrganizer(list);
        return list;
    }

    /** 校验活动归属：只能操作自己发布的活动 */
    public Activity requireOwned(Long id, Long operatorId) {
        Activity a = detail(id);
        if (a.getOrganizerId() == null || !a.getOrganizerId().equals(operatorId)) {
            throw new RuntimeException("只能管理自己发布的活动");
        }
        return a;
    }

    public Activity detail(Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null) throw new RuntimeException("活动不存在或已删除");
        fillOrganizer(List.of(a));
        return a;
    }

    public void create(Activity a, Long organizerId) {
        a.setId(null);
        a.setSignedCount(0);
        a.setOrganizerId(organizerId);
        a.setCreateTime(null);
        if (!StringUtils.hasText(a.getStatus())) a.setStatus("published");
        if (a.getMaxCount() == null || a.getMaxCount() <= 0) a.setMaxCount(20);
        if (a.getHours() == null || a.getHours() <= 0) a.setHours(1);
        if (!StringUtils.hasText(a.getCover())) a.setCover(coverByCategory(a.getCategory()));
        validate(a);
        activityMapper.insert(a);
    }

    public void update(Activity a, Long operatorId) {
        requireOwned(a.getId(), operatorId);
        if (a.getHours() == null) a.setHours(detail(a.getId()).getHours());
        validate(a);
        Activity old = detail(a.getId());
        if (a.getMaxCount() != null && a.getMaxCount() < old.getSignedCount()) {
            throw new RuntimeException("名额上限不能小于已报名人数（" + old.getSignedCount() + "）");
        }
        a.setSignedCount(old.getSignedCount());
        a.setOrganizerId(old.getOrganizerId());
        a.setCreateTime(old.getCreateTime());
        if (!StringUtils.hasText(a.getStatus())) a.setStatus(old.getStatus());
        if (!StringUtils.hasText(a.getCover())) a.setCover(old.getCover());
        activityMapper.updateById(a);
    }

    public void changeStatus(Long id, String status, Long operatorId) {
        requireOwned(id, operatorId);
        if (!"published".equals(status) && !"draft".equals(status)
                && !"closed".equals(status) && !"cancelled".equals(status)) {
            throw new RuntimeException("不支持的活动状态：" + status);
        }
        Activity a = detail(id);
        if (a.getEndTime() != null && a.getEndTime().isBefore(LocalDateTime.now())
                && "published".equals(status)) {
            throw new RuntimeException("活动已结束，不能重新上架");
        }
        a.setStatus(status);
        activityMapper.updateById(a);
    }

    public void delete(Long id, Long operatorId) {
        requireOwned(id, operatorId);
        activityMapper.deleteById(id);
    }

    /** 分类 → 内置封面名，前端 assets/covers 下同名图片 */
    public static String coverByCategory(String category) {
        if (category == null) return "community";
        switch (category) {
            case "医疗健康": return "medical";
            case "文化服务": return "culture";
            case "公益宣传": return "publicity";
            case "社区服务": return "community";
            case "教育助学": return "education";
            case "环境保护": return "environment";
            case "应急救援": return "rescue";
            default: return "community";
        }
    }

    private void validate(Activity a) {
        if (!StringUtils.hasText(a.getTitle())) throw new RuntimeException("活动名称不能为空");
        if (!StringUtils.hasText(a.getLocation())) throw new RuntimeException("活动地点不能为空");
        if (a.getStartTime() == null || a.getEndTime() == null) throw new RuntimeException("请填写活动起止时间");
        if (!a.getEndTime().isAfter(a.getStartTime())) throw new RuntimeException("结束时间必须晚于开始时间");
        if (a.getSignupDeadline() != null && a.getSignupDeadline().isAfter(a.getStartTime())) {
            throw new RuntimeException("报名截止时间不能晚于活动开始时间");
        }
        if (a.getHours() == null || a.getHours() <= 0) throw new RuntimeException("认定工时必须大于 0");
        if (a.getMaxCount() == null || a.getMaxCount() <= 0) throw new RuntimeException("招募人数必须大于 0");
    }

    /** 补充发布人昵称/头像，并计算是否已结束 */
    private void fillOrganizer(List<Activity> list) {
        LocalDateTime now = LocalDateTime.now();
        for (Activity a : list) {
            a.setEnded(a.getEndTime() != null && a.getEndTime().isBefore(now));
            if (a.getOrganizerId() != null) {
                User u = userMapper.selectById(a.getOrganizerId());
                if (u != null) {
                    a.setOrganizerName(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
                    a.setOrganizerAvatar(u.getAvatar());
                }
            }
        }
    }
}
