package com.volunteer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.volunteer.entity.ConversationPref;
import com.volunteer.mapper.ConversationPrefMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConversationService {

    @Autowired
    private ConversationPrefMapper mapper;

    public ConversationPref get(Long userId, String type, Long targetId) {
        return mapper.selectOne(new LambdaQueryWrapper<ConversationPref>()
                .eq(ConversationPref::getUserId, userId)
                .eq(ConversationPref::getConvType, type)
                .eq(ConversationPref::getTargetId, targetId));
    }

    public boolean isPinned(Long userId, String type, Long targetId) {
        ConversationPref p = get(userId, type, targetId);
        return p != null && Boolean.TRUE.equals(p.getPinned());
    }

    public LocalDateTime deletedTime(Long userId, String type, Long targetId) {
        ConversationPref p = get(userId, type, targetId);
        return p == null ? null : p.getDeletedTime();
    }

    public void setPinned(Long userId, String type, Long targetId, boolean pinned) {
        ConversationPref p = get(userId, type, targetId);
        if (p == null) {
            p = new ConversationPref();
            p.setUserId(userId);
            p.setConvType(type);
            p.setTargetId(targetId);
            p.setPinned(pinned);
            p.setUpdateTime(LocalDateTime.now());
            mapper.insert(p);
        } else {
            p.setPinned(pinned);
            p.setUpdateTime(LocalDateTime.now());
            mapper.updateById(p);
        }
    }

    public void delete(Long userId, String type, Long targetId) {
        ConversationPref p = get(userId, type, targetId);
        if (p == null) {
            p = new ConversationPref();
            p.setUserId(userId);
            p.setConvType(type);
            p.setTargetId(targetId);
            p.setPinned(false);
            p.setDeletedTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            mapper.insert(p);
        } else {
            p.setDeletedTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            mapper.updateById(p);
        }
    }
}
