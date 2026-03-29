package com.cmbchina.termgoal.job;

import com.cmbchina.termgoal.entity.MonthlyMonitoring;
import com.cmbchina.termgoal.mapper.MonthlyMonitoringMapper;
import com.cmbchina.termgoal.service.DataProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataProcessingJob {

    @Autowired
    private MonthlyMonitoringMapper monitoringMapper;

    @Autowired
    private DataProcessingService dataProcessingService;

    @Scheduled(fixedRate = 10000)
    public void processClosedMonitorings() {
        List<MonthlyMonitoring> monitorings = monitoringMapper.selectByStatusAndProcessStatus(
                MonthlyMonitoring.STATUS_CLOSED,
                MonthlyMonitoring.PROCESS_IDLE
        );

        for (MonthlyMonitoring monitoring : monitorings) {
            try {
                monitoringMapper.updateProcessStatus(monitoring.getId(),
                        MonthlyMonitoring.PROCESS_PROCESSING, 0, "Processing started");
                dataProcessingService.processMonitoringData(monitoring.getId());
            } catch (Exception e) {
                monitoringMapper.updateProcessStatus(monitoring.getId(),
                        MonthlyMonitoring.PROCESS_FAILED, 0, e.getMessage());
            }
        }
    }

    @Scheduled(fixedRate = 10000)
    public void updateProcessingProgress() {
        List<MonthlyMonitoring> processingMonitorings = monitoringMapper.selectByStatusAndProcessStatus(
                MonthlyMonitoring.STATUS_PROCESSING,
                MonthlyMonitoring.PROCESS_PROCESSING
        );

        for (MonthlyMonitoring monitoring : processingMonitorings) {
            dataProcessingService.updateProgress(monitoring.getId());
        }
    }
}
