package com.volunteer.task;

import com.volunteer.service.AttendanceService;
import com.volunteer.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：
 * 1) 启动时把「已完成活动 + 已通过报名 + 无工时」的历史数据补齐（解决"做了活动却没时长"）；
 * 2) 每 10 分钟跑一次：补工时 + 信誉分恢复（低于 85 分每 2 小时 +1，封顶 85）。
 */
@Component
public class ScheduledTasks implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        // 工时与信誉分改由组织者在活动结束后手动认定，不再自动补齐
    }

    @Scheduled(fixedDelay = 600000, initialDelay = 60000)
    public void tick() {
        try {
            int recovered = userService.recoverCredits();
            if (recovered > 0) {
                log.info("[予光] 定时任务：恢复信誉分 {} 人", recovered);
            }
        } catch (Exception e) {
            log.warn("[予光] 定时任务异常：{}", e.getMessage());
        }
    }
}
