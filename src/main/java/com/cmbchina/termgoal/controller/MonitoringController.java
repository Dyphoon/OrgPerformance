package com.cmbchina.termgoal.controller;

import com.cmbchina.termgoal.dto.MonitoringCreateRequest;
import com.cmbchina.termgoal.dto.TaskUpdateRequest;
import com.cmbchina.termgoal.entity.MonthlyMonitoring;
import com.cmbchina.termgoal.service.DataProcessingService;
import com.cmbchina.termgoal.service.MonitoringService;
import com.cmbchina.termgoal.vo.ApiResponse;
import com.cmbchina.termgoal.vo.CollectionTaskVO;
import com.cmbchina.termgoal.vo.ConfirmationTaskVO;
import com.cmbchina.termgoal.vo.MonitoringVO;
import com.cmbchina.termgoal.vo.PageVO;
import com.cmbchina.termgoal.mapper.SysUserMapper;
import com.cmbchina.termgoal.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/monitorings")
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private DataProcessingService dataProcessingService;

    @Autowired
    private SysUserMapper userMapper;

    @GetMapping
    public ApiResponse<PageVO<List<MonitoringVO>>> getMonitoringList(
            @RequestParam(required = false) Long systemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<MonitoringVO> list = monitoringService.getMonitoringList(systemId, status, year, month, page, pageSize);
        int total = monitoringService.count(systemId, status);
        return ApiResponse.success(new PageVO<>((long) total, page, pageSize, list));
    }

    @GetMapping("/{id}")
    public ApiResponse<MonitoringVO> getMonitoringById(@PathVariable Long id) {
        MonitoringVO monitoring = monitoringService.getMonitoringById(id);
        if (monitoring == null) {
            return ApiResponse.error(404, "Monitoring not found");
        }
        return ApiResponse.success(monitoring);
    }

    @PostMapping
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Long> createMonitoring(@RequestBody MonitoringCreateRequest request,
                                              Authentication authentication) throws IOException {
        String createdBy = authentication.getName();
        Long monitoringId = monitoringService.createMonitoring(request, createdBy);
        return ApiResponse.success("Monitoring created", monitoringId);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> closeMonitoring(@PathVariable Long id) {
        monitoringService.closeMonitoring(id);
        return ApiResponse.success("Monitoring closed");
    }

    @PostMapping("/{id}/start-confirming")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> startConfirming(@PathVariable Long id) {
        monitoringService.startConfirming(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('1', '3')")
    public ApiResponse<String> confirmInstitution(
            @PathVariable Long id,
            @RequestParam Long institutionId,
            Authentication authentication,
            @RequestParam(required = false) String remark) {
        String username = authentication.getName();
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            return ApiResponse.error(400, "用户不存在");
        }
        monitoringService.confirmInstitution(id, institutionId, user.getId(), remark);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> publishMonitoring(@PathVariable Long id) {
        if (!monitoringService.isAllConfirmed(id)) {
            return ApiResponse.error(400, "Not all institutions confirmed");
        }
        monitoringService.publishMonitoring(id);
        return ApiResponse.success("Published successfully");
    }

    @GetMapping("/{id}/process-status")
    public ApiResponse<MonitoringVO> getProcessStatus(@PathVariable Long id) {
        MonitoringVO monitoring = monitoringService.getMonitoringById(id);
        return ApiResponse.success(monitoring);
    }

    @GetMapping("/{id}/tasks")
    public ApiResponse<List<CollectionTaskVO>> getTasks(@PathVariable Long id,
            @RequestParam(required = false) String status) {
        List<CollectionTaskVO> tasks = dataProcessingService.getTasksByMonitoringId(id, status);
        return ApiResponse.success(tasks);
    }

    @GetMapping("/{id}/all-tasks")
    public ApiResponse<List<CollectionTaskVO>> getAllTasks(@PathVariable Long id) {
        List<CollectionTaskVO> tasks = dataProcessingService.getTasksByMonitoringId(id, null);
        return ApiResponse.success(tasks);
    }

    @GetMapping("/{id}/my-tasks")
    public ApiResponse<List<CollectionTaskVO>> getMyTasks(
            @PathVariable Long id,
            @RequestParam Long collectorUserId) {
        List<CollectionTaskVO> tasks = dataProcessingService.getTasksByCollector(id, collectorUserId);
        return ApiResponse.success(tasks);
    }

    @PutMapping("/tasks/{taskId}")
    public ApiResponse<String> submitTask(
            @PathVariable Long taskId,
            @RequestBody java.math.BigDecimal actualValue) {
        dataProcessingService.submitTaskData(taskId, actualValue);
        return ApiResponse.success();
    }

    @PutMapping("/tasks/batch")
    public ApiResponse<String> batchSubmitTasks(@RequestBody List<TaskUpdateRequest> updates) {
        int count = dataProcessingService.batchSubmitTasks(updates);
        return ApiResponse.success("Updated " + count + " tasks");
    }

    @PostMapping("/{id}/collect/upload")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> uploadDataCollection(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        dataProcessingService.uploadDataCollection(id, file);
        return ApiResponse.success("Data uploaded");
    }

    @PostMapping("/{id}/fix-collector-tasks")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> fixCollectorTasks(@PathVariable Long id) {
        int count = monitoringService.fixCollectorTasks(id);
        return ApiResponse.success("Fixed " + count + " tasks");
    }

    @GetMapping("/{id}/collector-file")
    public ApiResponse<String> getCollectorFileUrl(
            @PathVariable Long id,
            @RequestParam Long collectorUserId) throws IOException {
        String fileKey = dataProcessingService.getCollectorFileKey(id, collectorUserId);
        if (fileKey == null) {
            return ApiResponse.error(404, "Collector file not found");
        }
        String url = dataProcessingService.getCollectorFileUrl(fileKey);
        return ApiResponse.success(url);
    }

    @PostMapping("/{id}/generate-reports")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Integer> generateReports(@PathVariable Long id) throws IOException {
        int count = dataProcessingService.generateInstitutionReports(id);
        return ApiResponse.success("Generated " + count + " reports", count);
    }

    @PostMapping("/{id}/batch-generate")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> batchGenerateReports(@PathVariable Long id) throws IOException {
        monitoringService.batchGenerateReports(id);
        return ApiResponse.success("Reports generation started");
    }

    @GetMapping("/{id}/confirmation-tasks")
    public ApiResponse<List<ConfirmationTaskVO>> getConfirmationTasks(@PathVariable Long id) {
        List<ConfirmationTaskVO> tasks = monitoringService.getConfirmationTasks(id);
        return ApiResponse.success(tasks);
    }

    @PostMapping("/{id}/regenerate-confirmation-tasks")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> regenerateConfirmationTasks(@PathVariable Long id) {
        monitoringService.regenerateConfirmationTasks(id);
        return ApiResponse.success("确认任务已重新生成");
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> rollbackToCollecting(@PathVariable Long id) {
        monitoringService.rollbackToCollecting(id);
        return ApiResponse.success("已回退到收数中状态");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<String> deleteMonitoring(@PathVariable Long id) throws IOException {
        monitoringService.deleteMonitoring(id);
        return ApiResponse.success("删除成功");
    }
}
