package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.volunteer.entity.Activity;
import com.volunteer.entity.Message;
import com.volunteer.entity.Registration;
import com.volunteer.entity.User;
import com.volunteer.mapper.ActivityMapper;
import com.volunteer.mapper.MessageMapper;
import com.volunteer.mapper.RegistrationMapper;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class MessageService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private ActivityMapper activityMapper;
    @Autowired
    private ConversationService conversationService;

    public Long send(Long fromId, Long toId, String content) {
        if (toId == null) throw new RuntimeException("请选择聊天对象");
        if (content == null || content.trim().isEmpty()) throw new RuntimeException("消息不能为空");
        if (fromId == null) throw new RuntimeException("请先登录");
        if (fromId.equals(toId)) throw new RuntimeException("不能给自己发消息");

        String text = content.trim();
        if (text.length() > 500) text = text.substring(0, 500);

        User me = userMapper.selectById(fromId);
        User target = userMapper.selectById(toId);
        if (me == null) throw new RuntimeException("账号不存在");
        if (target == null) throw new RuntimeException("对方账号不存在");

        boolean pair =
                ("volunteer".equals(me.getRole()) && "organizer".equals(target.getRole()))
                || ("organizer".equals(me.getRole()) && "volunteer".equals(target.getRole()));
        if (!pair) throw new RuntimeException("私聊只在志愿者与组织者之间开放");

        Message m = new Message();
        m.setSenderId(fromId);
        m.setReceiverId(toId);
        m.setContent(text);
        m.setIsRead(0);
        messageMapper.insert(m);
        return m.getId();
    }

    /** 可联系的人：志愿者可找组织者；组织者可找报名过自己活动的志愿者；再加上已有聊天记录的人 */
    public List<Map<String, Object>> contacts(Long uid) {
        User me = userMapper.selectById(uid);
        String role = me == null ? "" : me.getRole();

        Set<Long> ids = new LinkedHashSet<>();
        Map<Long, String> reason = new HashMap<>();

        if ("volunteer".equals(role)) {
            List<User> organizers = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "organizer"));
            for (User u : organizers) {
                if (u.getId().equals(uid)) continue;
                ids.add(u.getId());
                reason.put(u.getId(), "组织者");
            }
        } else if ("organizer".equals(role)) {
            List<Activity> acts = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                    .eq(Activity::getOrganizerId, uid));
            List<Long> actIds = acts.stream().map(Activity::getId).toList();
            if (!actIds.isEmpty()) {
                List<Registration> regs = registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                        .in(Registration::getActivityId, actIds)
                        .ne(Registration::getStatus, "rejected"));
                for (Registration r : regs) {
                    if (r.getUserId().equals(uid)) continue;
                    ids.add(r.getUserId());
                    reason.put(r.getUserId(), "报名过你的活动");
                }
            }
        }

        // 已有私聊记录的人也要出现在列表里
        List<Message> partners = messageMapper.selectList(new QueryWrapper<Message>()
                .and(w -> w.eq("sender_id", uid)).or(w -> w.eq("receiver_id", uid)));
        for (Message m : partners) {
            Long other = m.getSenderId().equals(uid) ? m.getReceiverId() : m.getSenderId();
            if (!other.equals(uid)) ids.add(other);
        }

        String needRole = "volunteer".equals(role) ? "organizer" : "volunteer";

        List<Map<String, Object>> res = new ArrayList<>();
        for (Long otherId : ids) {
            User u = userMapper.selectById(otherId);
            if (u == null || "admin".equals(u.getRole())) continue;

            Message last = messageMapper.selectOne(new QueryWrapper<Message>()
                    .and(w -> w.eq("sender_id", uid).eq("receiver_id", otherId))
                    .or(w -> w.eq("sender_id", otherId).eq("receiver_id", uid))
                    .orderByDesc("create_time").last("limit 1"));

            // 已删除的会话：没有新消息就继续隐藏
            LocalDateTime deleted = conversationService.deletedTime(uid, "dm", otherId);
            if (deleted != null && (last == null || last.getCreateTime() == null
                    || !last.getCreateTime().isAfter(deleted))) {
                continue;
            }

            // 只展示可对话的角色；已有聊天记录的人保留
            if (last == null && !needRole.equals(u.getRole())) continue;

            long unread = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                    .eq(Message::getSenderId, otherId)
                    .eq(Message::getReceiverId, uid)
                    .eq(Message::getIsRead, 0));

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getNickname() == null || u.getNickname().isBlank() ? u.getUsername() : u.getNickname());
            m.put("username", u.getUsername());
            m.put("role", u.getRole());
            m.put("avatar", u.getAvatar());
            m.put("creditScore", u.getCreditScore());
            m.put("reason", reason.getOrDefault(otherId, "组织者".equals(u.getRole()) ? "组织者" : "志愿者"));
            m.put("unread", unread);
            m.put("pinned", conversationService.isPinned(uid, "dm", otherId));
            m.put("type", "dm");
            m.put("lastText", last == null ? null : last.getContent());
            m.put("lastTime", last == null || last.getCreateTime() == null ? null : last.getCreateTime().format(FMT));
            res.add(m);
        }

        res.sort((a, b) -> {
            boolean pa = Boolean.TRUE.equals(a.get("pinned"));
            boolean pb = Boolean.TRUE.equals(b.get("pinned"));
            if (pa != pb) return pa ? -1 : 1;
            String ta = (String) a.get("lastTime");
            String tb = (String) b.get("lastTime");
            if (ta == null && tb == null) return ((String) a.get("name")).compareTo((String) b.get("name"));
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });
        return res;
    }

    public List<Map<String, Object>> thread(Long uid, Long otherId) {
        if (otherId == null) throw new RuntimeException("缺少聊天对象");

        // 打开会话即把对方发来的消息标记为已读
        messageMapper.update(null, new UpdateWrapper<Message>()
                .eq("sender_id", otherId)
                .eq("receiver_id", uid)
                .eq("is_read", 0)
                .set("is_read", 1));

        List<Message> list = messageMapper.selectList(new QueryWrapper<Message>()
                .and(w -> w.eq("sender_id", uid).eq("receiver_id", otherId))
                .or(w -> w.eq("sender_id", otherId).eq("receiver_id", uid))
                .orderByAsc("create_time")
                .last("limit 300"));

        List<Map<String, Object>> res = new ArrayList<>();
        for (Message m : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("mine", m.getSenderId().equals(uid));
            item.put("content", m.getContent());
            item.put("time", m.getCreateTime() == null ? "" : m.getCreateTime().format(FMT));
            item.put("isRead", m.getIsRead());
            res.add(item);
        }
        return res;
    }

    public long unread(Long uid) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, uid)
                .eq(Message::getIsRead, 0));
    }

    /** 删除会话时清掉与这个人的未读，避免导航红点残留 */
    public void markReadWith(Long uid, Long otherId) {
        messageMapper.update(null, new UpdateWrapper<Message>()
                .eq("sender_id", otherId)
                .eq("receiver_id", uid)
                .eq("is_read", 0)
                .set("is_read", 1));
    }
}
