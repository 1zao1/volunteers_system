package com.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/** 志愿者对组织者的活动评价：1-5 星，1-2 星视为差评 */
@Data
@TableName("organizer_review")
public class OrganizerReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long registrationId;
    private Long activityId;
    private Long volunteerId;
    private Long organizerId;
    private Integer rating;
    private String comment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createTime;
}
