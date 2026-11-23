package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.volunteer.entity.Activity;
import com.volunteer.entity.ActivityChat;
import com.volunteer.entity.ActivityChatRead;
import com.volunteer.entity.Registration;
import com.volunteer.entity.User;
import com.volunteer.mapper.ActivityChatMapper;
import com.volunteer.mapper.ActivityChatReadMapper;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.mapper.RegistrationMapper;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ActivityChatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired private ActivityChatMapper chatMapper;
    @Autowired private ActivityChatReadMapper readMapper;
    @Autowired private ActivityMapper activityMapper;
    @Autowired private RegistrationMapper regMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private ConversationService conversationService;

    /** 群成员：活动组织者 + 审核通过的志愿者 */
    private List<User> members(Activity act) {
        List<User> res = new ArrayList<>();
        User org = userMapper.selectById(act.getOrganizerId());
        if (org != null) res.add(org);
        List<Registration> regs = regMapper.selectList(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, act.getId())
                .eq(Registration::getStatus, "approved"));
        Set<Long> seen = new HashSet<>();
        for (Registration r : regs) {
            if (!seen.add(r.getUserId())) continue;
            User u = userMapper.selectById(r.getUserId());
            if (u != null) res.add(u);
        }
        return res;
    }

    private Activity requireMember(Long activityId, Long uid) {
        Activity act = activityMapper.selectById(activityId);
        if (act == null) throw new RuntimeException("活动不存在或已删除");
        if (act.getOrganizerId() != null && act.getOrganizerId().equals(uid)) return act;
        long mine = regMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, uid)
                .eq(Registration::getStatus, "approved"));
        if (mine == 0) throw new RuntimeException("审核通过后才能进入活动群聊");
        return act;
    }

    private Long maxMessageId(Long activityId) {
        ActivityChat last = chatMapper.selectOne(new QueryWrapper<ActivityChat>()
                .eq("activity_id", activityId)
                .orderByDesc("id")
                .last("limit 1"));
        return last == null ? 0L : last.getId();
    }

    private long lastReadId(Long userId, Long activityId) {
        ActivityChatRead r = readMapper.selectOne(new LambdaQueryWrapper<ActivityChatRead>()
                .eq(ActivityChatRead::getUserId, userId)
                .eq(ActivityChatRead::getActivityId, activityId));
        return r == null || r.getLastReadId() == null ? 0L : r.getLastReadId();
    }

    /** 把已读位置推进到指定消息 id；只前进不后退 */
    public void markRead(Long activityId, Long userId, Long messageId) {
        if (activityId == null || userId == null) return;
        long target = messageId == null ? 0L : messageId;
        ActivityChatRead r = readMapper.selectOne(new LambdaQueryWrapper<ActivityChatRead>()
                .eq(ActivityChatRead::getUserId, userId)
                .eq(ActivityChatRead::getActivityId, activityId));
        if (r == null) {
            r = new ActivityChatRead();
            r.setUserId(userId);
            r.setActivityId(activityId);
            r.setLastReadId(target);
            r.setUpdateTime(LocalDateTime.now());
            readMapper.insert(r);
            return;
        }
        long current = r.getLastReadId() == null ? 0L : r.getLastReadId();
        if (target > current) {
            r.setLastReadId(target);
            r.setUpdateTime(LocalDateTime.now());
            readMapper.updateById(r);
        }
    }

    /** 审核通过 / 新入群时，把历史消息标成已读，只接收之后的新消息 */
    public void markReadToEnd(Long activityId, Long userId) {
        markRead(activityId, userId, maxMessageId(activityId));
    }

    private long unreadCount(Long activityId, Long userId) {
        long since = lastReadId(userId, activityId);
        return chatMapper.selectCount(new LambdaQueryWrapper<ActivityChat>()
                .eq(ActivityChat::getActivityId, activityId)
                .gt(ActivityChat::getId, since)
                .ne(ActivityChat::getSenderId, userId));
    }

    public List<Map<String, Object>> messages(Long activityId, Long uid) {
        requireMember(activityId, uid);
        List<ActivityChat> list = chatMapper.selectList(new QueryWrapper<ActivityChat>()
                .eq("activity_id", activityId)
                .orderByAsc("create_time")
                .last("limit 300"));
        List<Map<String, Object>> res = new ArrayList<>();
        for (ActivityChat c : list) {
            User u = userMapper.selectById(c.getSenderId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("mine", c.getSenderId().equals(uid));
            m.put("senderId", c.getSenderId());
            m.put("senderName", u == null ? "已注销用户"
                    : (u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname()));
            m.put("senderRole", u == null ? "volunteer" : u.getRole());
            m.put("content", c.getContent());
            m.put("time", c.getCreateTime() == null ? "" : c.getCreateTime().format(FMT));
            res.add(m);
        }
        if (!list.isEmpty()) {
            markRead(activityId, uid, list.get(list.size() - 1).getId());
        }
        return res;
    }

    public Long send(Long activityId, Long uid, String content) {
        requireMember(activityId, uid);
        if (content == null || content.trim().isEmpty()) throw new RuntimeException("消息不能为空");
        String text = content.trim();
        if (text.length() > 500) text = text.substring(0, 500);
        ActivityChat c = new ActivityChat();
        c.setActivityId(activityId);
        c.setSenderId(uid);
        c.setContent(text);
        chatMapper.insert(c);
        // 发送者自己已读到自己发的这条，避免给自己产生未读
        markRead(activityId, uid, c.getId());
        return c.getId();
    }

    public Map<String, Object> info(Long activityId, Long uid) {
        Activity act = requireMember(activityId, uid);
        List<User> members = members(act);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("activityId", act.getId());
        m.put("title", act.getTitle());
        m.put("cover", act.getCover());
        m.put("category", act.getCategory());
        m.put("status", act.getStatus());
        m.put("endTime", act.getEndTime() == null ? null : act.getEndTime().format(FMT));
        m.put("isOrganizer", act.getOrganizerId() != null && act.getOrganizerId().equals(uid));
        m.put("memberCount", members.size());
        m.put("members", members.stream().map(u -> {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("id", u.getId());
            x.put("name", u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname());
            x.put("role", u.getRole());
            return x;
        }).toList());
        return m;
    }

    /** 我参与或我组织的活动群列表 */
    public List<Map<String, Object>> myGroups(Long uid, String role) {
        Set<Long> ids = myActivityIds(uid, role);

        List<Map<String, Object>> res = new ArrayList<>();
        for (Long activityId : ids) {
            Activity a = activityMapper.selectById(activityId);
            if (a == null) continue;
            ActivityChat last = chatMapper.selectOne(new QueryWrapper<ActivityChat>()
                    .eq("activity_id", activityId)
                    .orderByDesc("create_time")
                    .last("limit 1"));
            LocalDateTime deleted = conversationService.deletedTime(uid, "group", activityId);
            if (deleted != null && (last == null || last.getCreateTime() == null
                    || !last.getCreateTime().isAfter(deleted))) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("title", a.getTitle());
            m.put("cover", a.getCover());
            m.put("category", a.getCategory());
            m.put("status", a.getStatus());
            m.put("memberCount", members(a).size());
            m.put("lastText", last == null ? null : last.getContent());
            m.put("lastTime", last == null || last.getCreateTime() == null ? null : last.getCreateTime().format(FMT));
            m.put("unread", unreadCount(activityId, uid));
            m.put("pinned", conversationService.isPinned(uid, "group", activityId));
            m.put("type", "group");
            res.add(m);
        }
        res.sort((x, y) -> {
            boolean px = Boolean.TRUE.equals(x.get("pinned"));
            boolean py = Boolean.TRUE.equals(y.get("pinned"));
            if (px != py) return px ? -1 : 1;
            String tx = (String) x.get("lastTime");
            String ty = (String) y.get("lastTime");
            if (tx == null && ty == null) return 0;
            if (tx == null) return 1;
            if (ty == null) return -1;
            return ty.compareTo(tx);
        });
        return res;
    }

    private Set<Long> myActivityIds(Long uid, String role) {
        Set<Long> ids = new LinkedHashSet<>();
        if ("organizer".equals(role)) {
            for (Activity a : activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                    .eq(Activity::getOrganizerId, uid)
                    .orderByDesc(Activity::getId))) {
                ids.add(a.getId());
            }
        } else {
            for (Registration r : regMapper.selectList(new LambdaQueryWrapper<Registration>()
                    .eq(Registration::getUserId, uid)
                    .eq(Registration::getStatus, "approved")
                    .orderByDesc(Registration::getId))) {
                ids.add(r.getActivityId());
            }
        }
        return ids;
    }

    /** 群聊未读总数，供导航栏红点使用 */
    public long unreadTotal(Long uid, String role) {
        long total = 0;
        for (Long activityId : myActivityIds(uid, role)) {
            total += unreadCount(activityId, uid);
        }
        return total;
    }
}
