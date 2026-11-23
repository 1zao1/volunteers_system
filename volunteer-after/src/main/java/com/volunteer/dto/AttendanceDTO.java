package com.volunteer.dto;

import lombok.Data;

@Data
public class AttendanceDTO {
    private Long registrationId;
    /** 不传则使用活动设置的工时 */
    private Integer hours;
    /** 组织者认定工时时同时给出的信誉分，不传则不修改 */
    private Integer creditScore;
}
