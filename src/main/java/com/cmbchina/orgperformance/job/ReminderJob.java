package com.cmbchina.orgperformance.job;

import com.cmbchina.orgperformance.entity.CollectionTask;
import com.cmbchina.orgperformance.entity.MonthlyMonitoring;
import com.cmbchina.orgperformance.mapper.CollectionTaskMapper;
import com.cmbchina.orgperformance.mapper.MonthlyMonitoringMapper;
import com.cmbchina.orgperformance.mapper.SysUserMapper;
import com.cmbchina.orgperformance.notify.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ReminderJob {

    @Autowired
    private MonthlyMonitoringMapper monitoringMapper;

    @Autowired
    private CollectionTaskMapper taskMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(fixedRate = 60000)
    public void sendDeadlineReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(30);

        List<MonthlyMonitoring> collectingMonitorings = monitoringMapper.selectByStatusAndProcessStatus(
                MonthlyMonitoring.STATUS_COLLECTING,
                MonthlyMonitoring.PROCESS_IDLE
        );

        for (MonthlyMonitoring monitoring : collectingMonitorings) {
            if (monitoring.getDeadline() != null &&
                    monitoring.getDeadline().isAfter(now) &&
                    monitoring.getDeadline().isBefore(threshold)) {

                List<CollectionTask> pendingTasks = taskMapper.selectByMonitoringIdAndStatus(
                        monitoring.getId(), CollectionTask.STATUS_PENDING);

                for (CollectionTask task : pendingTasks) {
                    if (task.getCollectorUserId() != null) {
                        String content = String.format(
                                "收数即将截止！体系：%s，月份：%d年%d月。请尽快提交数据。",
                                monitoring.getSystemId(), monitoring.getYear(), monitoring.getMonth());
                        notificationService.sendNotification(
                                task.getCollectorUserId(),
                                "收数截止提醒",
                                content,
                                "site"
                        );
                    }
                }
            }
        }
    }
}
