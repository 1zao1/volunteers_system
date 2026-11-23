package com.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String category;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;

    /** 报名截止时间，为空表示活动开始前都可报名 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime signupDeadline;

    private Integer maxCount;
    private Integer signedCount;
    private Integer hours;
    private String description;
    private String status;
    private Long organizerId;

    /** 封面：内置封面名（medical/culture/...）或图片 URL */
    private String cover;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createTime;

    /** 发布人昵称（非数据库字段，详情/列表时补充） */
    @TableField(exist = false)
    private String organizerName;

    /** 发布人头像（非数据库字段） */
    @TableField(exist = false)
    private String organizerAvatar;

    /** 是否已结束（非数据库字段，按结束时间计算） */
    @TableField(exist = false)
    private Boolean ended;
}
