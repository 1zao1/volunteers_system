package com.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity_chat_read")
public class ActivityChatRead {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long activityId;
    /** 已读到的最后一条群消息 id；0 表示还没读过 */
    private Long lastReadId;
    private LocalDateTime updateTime;
}
