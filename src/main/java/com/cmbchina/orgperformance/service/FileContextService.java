package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.util.DocumentExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileContextService {

    private static final Logger logger = LoggerFactory.getLogger(FileContextService.class);
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_TOTAL_CONTENT_SIZE = 50000;

    @Autowired
    private DocumentExtractor documentExtractor;

    private final Map<String, List<FileContextEntry>> sessionFileContexts = new ConcurrentHashMap<>();

    public record FileContextEntry(
        String fileName, 
        String content, 
        String fileType, 
        long fileSize, 
        long uploadedAt,
        String base64Content
    ) {}

    public record UploadResult(boolean success, String fileName, String fileType, long fileSize, 
                              int contentLength, String contentPreview, String error) {}

    public UploadResult uploadFile(MultipartFile file, String sessionId) {
        logger.info("File upload: sessionId={}, fileName={}, size={}", 
            sessionId, file.getOriginalFilename(), file.getSize());

        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isEmpty()) {
                return new UploadResult(false, null, null, 0, 0, null, "文件名不能为空");
            }

            if (!documentExtractor.isSupported(fileName)) {
                return new UploadResult(false, null, null, 0, 0, null, 
                    "不支持的文件格式，支持的格式: .doc, .docx, .xls, .xlsx, .ppt, .pptx");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                return new UploadResult(false, null, null, 0, 0, null, "文件大小超过限制(最大10MB)");
            }

            byte[] fileBytes = file.getBytes();
            String base64Content = Base64.getEncoder().encodeToString(fileBytes);
            
            DocumentExtractor.ExtractedContent extracted = documentExtractor.extract(fileBytes, fileName);

            FileContextEntry entry = new FileContextEntry(
                extracted.fileName(),
                extracted.content(),
                extracted.fileType(),
                extracted.fileSize(),
                System.currentTimeMillis(),
                base64Content
            );

            sessionFileContexts.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>())).add(entry);

            String preview = extracted.content().length() > 200 
                ? extracted.content().substring(0, 200) + "..." 
                : extracted.content();

            logger.info("File uploaded successfully: sessionId={}, fileName={}, contentLength={}", 
                sessionId, fileName, extracted.content().length());

            return new UploadResult(true, extracted.fileName(), extracted.fileType(), 
                extracted.fileSize(), extracted.content().length(), preview, null);

        } catch (Exception e) {
            logger.error("File upload failed: {}", e.getMessage(), e);
            return new UploadResult(false, null, null, 0, 0, null, "文件处理失败: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getSessionFiles(String sessionId) {
        List<FileContextEntry> files = sessionFileContexts.getOrDefault(sessionId, Collections.emptyList());
        return files.stream()
            .map(f -> Map.<String, Object>of(
                "fileName", f.fileName(),
                "fileType", f.fileType(),
                "fileSize", f.fileSize(),
                "contentLength", f.content.length(),
                "contentPreview", f.content.length() > 200 ? f.content.substring(0, 200) + "..." : f.content,
                "uploadedAt", f.uploadedAt
            ))
            .toList();
    }

    public Optional<String> getFileBase64(String sessionId, String fileName) {
        List<FileContextEntry> files = sessionFileContexts.get(sessionId);
        if (files != null) {
            return files.stream()
                .filter(f -> f.fileName().equals(fileName))
                .map(FileContextEntry::base64Content)
                .findFirst();
        }
        return Optional.empty();
    }

    public Optional<FileContextEntry> getFile(String sessionId, String fileName) {
        List<FileContextEntry> files = sessionFileContexts.get(sessionId);
        if (files != null) {
            return files.stream()
                .filter(f -> f.fileName().equals(fileName))
                .findFirst();
        }
        return Optional.empty();
    }

    public void clearSessionFiles(String sessionId) {
        sessionFileContexts.remove(sessionId);
        logger.info("Cleared file contexts for session: {}", sessionId);
    }

    public boolean removeFile(String sessionId, String fileName) {
        List<FileContextEntry> files = sessionFileContexts.get(sessionId);
        if (files != null) {
            boolean removed = files.removeIf(f -> f.fileName().equals(fileName));
            if (removed) {
                logger.info("Removed file {} from session {}", fileName, sessionId);
            }
            return removed;
        }
        return false;
    }

    public String buildFileContext(String sessionId) {
        List<FileContextEntry> files = sessionFileContexts.get(sessionId);
        if (files == null || files.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n\n【用户上传的文档内容】\n");
        context.append("以下文件内容供您参考，请根据这些内容回答用户的问题：\n\n");

        long totalLength = 0;
        for (FileContextEntry file : files) {
            if (totalLength >= MAX_TOTAL_CONTENT_SIZE) {
                context.append("\n[... 其他文档内容已截断 ...]\n");
                break;
            }

            context.append("--- ").append(file.fileName()).append(" (").append(file.fileType()).append(") ---\n");
            String content = file.content();
            
            if (totalLength + content.length() > MAX_TOTAL_CONTENT_SIZE) {
                int remaining = (int)(MAX_TOTAL_CONTENT_SIZE - totalLength);
                if (remaining > 100) {
                    context.append(content, 0, remaining).append("\n[... 内容已截断 ...]\n");
                }
                totalLength = MAX_TOTAL_CONTENT_SIZE;
            } else {
                context.append(content);
                totalLength += content.length();
            }
            context.append("\n\n");
        }

        return context.toString();
    }

    public int getFileCount(String sessionId) {
        List<FileContextEntry> files = sessionFileContexts.get(sessionId);
        return files != null ? files.size() : 0;
    }
}
