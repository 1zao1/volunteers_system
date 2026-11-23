package com.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_pref")
public class ConversationPref {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** dm = 私聊，group = 活动群聊 */
    private String convType;
    /** 私聊为对方用户 id，群聊为活动 id */
    private Long targetId;
    private Boolean pinned;
    /** 用户删除会话的时间；之后有新消息会重新出现 */
    private LocalDateTime deletedTime;
    private LocalDateTime updateTime;
}
