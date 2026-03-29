package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.dto.ApiResponse;
import com.cmbchina.orgperformance.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview(@RequestParam Long monitoringId) {
        try {
            Map<String, Object> overview = reportService.getOverview(monitoringId);
            return ApiResponse.success(overview);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/institution/{institutionId}")
    public ApiResponse<Map<String, Object>> getInstitutionReport(
            @PathVariable Long institutionId,
            @RequestParam Long monitoringId) {
        try {
            Map<String, Object> report = reportService.getInstitutionReport(monitoringId, institutionId);
            return ApiResponse.success(report);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/institution/{institutionId}/download")
    public void downloadInstitutionReport(
            @PathVariable Long institutionId,
            @RequestParam Long monitoringId,
            HttpServletResponse response) {
        try {
            InputStream inputStream = reportService.downloadInstitutionReport(monitoringId, institutionId);
            String fileName = reportService.getInstitutionReportFileName(monitoringId, institutionId);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            inputStream.close();
        } catch (Exception e) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "下载失败";
                response.getWriter().write("{\"error\":\"" + errorMsg + "\"}");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @GetMapping("/debug/file-key")
    public ApiResponse<String> debugFileKey(@RequestParam Long monitoringId, @RequestParam Long institutionId) {
        try {
            String fileKey = reportService.getFileKey(monitoringId, institutionId);
            return ApiResponse.success(fileKey);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/debug/bucket-config")
    public ApiResponse<Map<String, String>> debugBucketConfig() {
        try {
            Map<String, String> config = reportService.getBucketConfig();
            return ApiResponse.success(config);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/debug/monitorings")
    public ApiResponse<List<Map<String, Object>>> debugMonitorings(@RequestParam(required=false) String status) {
        try {
            List<Map<String, Object>> result = reportService.getMonitoringsWithReports(status);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/debug/minio-test")
    public ApiResponse<Map<String, Object>> debugMinioTest() {
        try {
            Map<String, Object> result = reportService.testMinioConnection();
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
