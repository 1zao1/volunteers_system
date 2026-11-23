package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.volunteer.common.JwtUtil;
import com.volunteer.dto.LoginDTO;
import com.volunteer.dto.LoginVO;
import com.volunteer.dto.ProfileDTO;
import com.volunteer.dto.RegisterDTO;
import com.volunteer.entity.User;
import com.volunteer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {

    /** 信誉分恢复上限：低于该分数时每 2 小时恢复 1 分，直到回到 85 */
    public static final int CREDIT_FLOOR = 85;
    /** 低于该分数视为「低信誉」，报名前需提示，但允许参加活动换取恢复 */
    public static final int CREDIT_BLOCK = 85;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtUtil;

    public void register(RegisterDTO dto) {
        if (!StringUtils.hasText(dto.getUsername()) || !StringUtils.hasText(dto.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new RuntimeException("用户名已存在，换一个试试");
        }
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setNickname(dto.getUsername());
        u.setPassword(dto.getPassword());
        u.setPhone(dto.getPhone());
        String role = StringUtils.hasText(dto.getRole()) ? dto.getRole().trim() : "volunteer";
        if (!Arrays.asList("volunteer", "organizer").contains(role)) {
            throw new RuntimeException("注册角色只能是 volunteer / organizer");
        }
        u.setRole(role);
        u.setCreditScore(100);
        u.setCreditTime(LocalDateTime.now());
        u.setSkills(joinSkills(dto.getSkills()));
        userMapper.insert(u);
    }

    private String joinSkills(Object skills) {
        if (skills == null) return null;
        if (skills instanceof java.util.Collection<?> c) {
            return c.stream()
                    .map(s -> s == null ? "" : String.valueOf(s).trim())
                    .filter(StringUtils::hasText)
                    .collect(java.util.stream.Collectors.joining(","));
        }
        String s = String.valueOf(skills).trim();
        return StringUtils.hasText(s) ? s : null;
    }

    public LoginVO login(LoginDTO dto) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (u == null || !u.getPassword().equals(dto.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        // 平台只保留志愿者与组织者；历史上残留的 admin 账号按组织者身份继续使用
        String role = u.getRole();
        if ("admin".equals(role) || "admin_pending".equals(role)) {
            role = "organizer";
            u.setRole(role);
            userMapper.updateById(u);
        }
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generate(u.getId(), role));
        vo.setId(u.getId());
        vo.setName(u.getUsername());
        vo.setNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        vo.setAvatar(u.getAvatar());
        vo.setSignature(u.getSignature());
        vo.setCreditScore(u.getCreditScore());
        vo.setRole(role);
        return vo;
    }

    public User get(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) throw new RuntimeException("用户不存在");
        return u;
    }

    public Map<String, Object> profile(Long userId) {
        User u = get(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());
        m.put("signature", u.getSignature());
        m.put("avatar", u.getAvatar());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole());
        m.put("skills", u.getSkills());
        m.put("creditScore", u.getCreditScore());
        m.put("creditDesc", creditDesc(u.getCreditScore()));
        m.put("createTime", u.getCreateTime());
        return m;
    }

    public String creditDesc(Integer score) {
        int c = score == null ? 100 : score;
        if (c >= 95) return "信誉良好，可同时报名多个活动";
        if (c >= 85) return "信誉正常，再违约一次将进入低信誉";
        if (c >= 60) return "低信誉，同时只能保留 1 个未完成报名，每 2 小时恢复 1 分";
        return "失信状态，暂停报名新活动，每 2 小时恢复 1 分";
    }

    public void updateProfile(Long userId, ProfileDTO dto) {
        User u = get(userId);
        if (dto.getNickname() != null) {
            String n = dto.getNickname().trim();
            if (n.length() > 20) throw new RuntimeException("昵称最多 20 个字");
            u.setNickname(StringUtils.hasText(n) ? n : u.getUsername());
        }
        if (dto.getSignature() != null) {
            String s = dto.getSignature().trim();
            if (s.length() > 80) throw new RuntimeException("个性签名最多 80 个字");
            u.setSignature(s);
        }
        if (dto.getAvatar() != null) {
            String a = dto.getAvatar().trim();
            if (a.length() > 300000) throw new RuntimeException("头像图片过大，请换一张小一点的");
            u.setAvatar(a);
        }
        userMapper.updateById(u);
    }

    /** 违约扣分，并重置恢复计时锚点 */
    public void deductCredit(Long userId, int amount) {
        User u = userMapper.selectById(userId);
        if (u == null) return;
        int old = u.getCreditScore() == null ? 100 : u.getCreditScore();
        int now = Math.max(0, old - amount);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getCreditScore, now)
                .set(User::getCreditTime, LocalDateTime.now()));
    }

    /**
     * 信誉分恢复：低于 85 分时每满 2 小时恢复 1 分，直到 85 分封顶。
     */
    public int recoverCredits() {
        int fixed = 0;
        LocalDateTime now = LocalDateTime.now();
        for (User u : userMapper.selectList(new LambdaQueryWrapper<User>()
                .lt(User::getCreditScore, CREDIT_FLOOR))) {
            LocalDateTime anchor = u.getCreditTime() != null ? u.getCreditTime() : now;
            long minutes = Duration.between(anchor, now).toMinutes();
            long steps = minutes / 120;
            if (steps <= 0) continue;
            int score = Math.min(CREDIT_FLOOR, (u.getCreditScore() == null ? 0 : u.getCreditScore()) + (int) steps);
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, u.getId())
                    .set(User::getCreditScore, score)
                    .set(User::getCreditTime, anchor.plusMinutes(steps * 120)));
            fixed++;
        }
        return fixed;
    }
}
