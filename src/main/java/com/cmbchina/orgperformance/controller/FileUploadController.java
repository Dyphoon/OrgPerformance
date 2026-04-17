package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.service.AssessmentSystemService;
import com.cmbchina.orgperformance.service.FileContextService;
import com.cmbchina.orgperformance.service.TemplateValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private FileContextService fileContextService;

    @Autowired
    private AssessmentSystemService systemService;

    @Autowired
    private TemplateValidationService templateValidationService;

    @PostMapping("/upload")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file, 
                                          @RequestParam("sessionId") String sessionId) {
        logger.info("File upload request: sessionId={}, fileName={}, size={}", 
            sessionId, file.getOriginalFilename(), file.getSize());

        String fileName = file.getOriginalFilename();
        String extension = fileName != null ? fileName.substring(fileName.lastIndexOf('.')).toLowerCase() : "";
        
        boolean isExcel = extension.equals(".xlsx") || extension.equals(".xls");
        
        FileContextService.UploadResult result = fileContextService.uploadFile(file, sessionId);

        if (!result.success()) {
            return Map.of("success", false, "error", result.error());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", sessionId);
        response.put("fileName", result.fileName());
        response.put("fileType", result.fileType());
        response.put("fileSize", result.fileSize());
        response.put("contentLength", result.contentLength());
        response.put("contentPreview", result.contentPreview());
        
        if (isExcel) {
            try {
                String fileKey = systemService.uploadTemplate(file);
                var validationResult = templateValidationService.validate(file.getInputStream());
                response.put("templateFileKey", fileKey);
                response.put("isTemplate", true);
                response.put("templateValid", validationResult.isValid());
                if (!validationResult.isValid()) {
                    response.put("templateErrors", validationResult.getErrors());
                }
                logger.info("Excel template processed: sessionId={}, fileName={}, fileKey={}", 
                    sessionId, fileName, fileKey);
            } catch (Exception e) {
                logger.error("Failed to process Excel template: {}", e.getMessage());
                response.put("templateError", e.getMessage());
            }
        }

        return response;
    }

    @PostMapping("/files")
    public Map<String, Object> getSessionFiles(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId == null) {
            return Map.of("success", false, "error", "sessionId is required");
        }

        List<Map<String, Object>> fileList = fileContextService.getSessionFiles(sessionId);
        return Map.of("success", true, "files", fileList, "count", fileList.size());
    }

    @PostMapping("/files/clear")
    public Map<String, Object> clearSessionFiles(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId != null) {
            fileContextService.clearSessionFiles(sessionId);
            logger.info("Cleared file contexts for session: {}", sessionId);
        }
        return Map.of("success", true);
    }

    @PostMapping("/files/remove")
    public Map<String, Object> removeFile(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String fileName = request.get("fileName");
        
        if (sessionId == null || fileName == null) {
            return Map.of("success", false, "error", "sessionId and fileName are required");
        }

        boolean removed = fileContextService.removeFile(sessionId, fileName);
        return Map.of("success", removed);
    }
}
