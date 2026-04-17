package com.cmbchina.orgperformance.util;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.sl.usermodel.Slide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DocumentExtractor.class);

    private static final String[] WORD_EXTENSIONS = {".docx", ".doc"};
    private static final String[] EXCEL_EXTENSIONS = {".xlsx", ".xls"};
    private static final String[] PPT_EXTENSIONS = {".pptx", ".ppt"};

    public record ExtractedContent(String fileName, String content, String fileType, long fileSize) {}

    public ExtractedContent extract(byte[] fileData, String fileName) throws IOException {
        String extension = getFileExtension(fileName).toLowerCase();
        
        String content;
        if (extension.equals(".docx")) {
            content = extractFromDocx(fileData);
        } else if (extension.equals(".doc")) {
            content = extractFromOldDoc(fileData);
        } else if (extension.equals(".xlsx")) {
            content = extractFromXlsx(fileData);
        } else if (extension.equals(".xls")) {
            content = extractFromXls(fileData);
        } else if (extension.equals(".pptx")) {
            content = extractFromPptx(fileData);
        } else if (extension.equals(".ppt")) {
            content = extractFromOldPpt(fileData);
        } else {
            throw new IOException("Unsupported file type: " + extension);
        }

        return new ExtractedContent(fileName, content, extension, fileData.length);
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    private String extractFromDocx(byte[] fileData) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileData));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        } catch (Exception e) {
            logger.error("Error extracting DOCX: {}", e.getMessage());
            throw new IOException("Failed to extract DOCX content: " + e.getMessage(), e);
        }
    }

    private String extractFromOldDoc(byte[] fileData) throws IOException {
        try (HSLFSlideShow slideShow = new HSLFSlideShow(new ByteArrayInputStream(fileData))) {
            StringBuilder content = new StringBuilder();
            for (HSLFSlide slide : slideShow.getSlides()) {
                for (Object obj : slide.getShapes()) {
                    if (obj instanceof HSLFTextShape) {
                        HSLFTextShape ts = (HSLFTextShape) obj;
                        String text = ts.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\n");
                        }
                    }
                }
            }
            if (content.length() == 0) {
                throw new IOException("无法读取此文档内容，可能文件已损坏或格式不支持");
            }
            return content.toString();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error extracting old DOC: {}", e.getMessage());
            throw new IOException("Failed to extract DOC content: " + e.getMessage(), e);
        }
    }

    private String extractFromXlsx(byte[] fileData) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileData))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("【Sheet: ").append(sheet.getSheetName()).append("】\n");
                
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        rowData.add(getCellValueAsString(cell));
                    }
                    if (!rowData.isEmpty() && rowData.stream().anyMatch(v -> v != null && !v.isEmpty())) {
                        content.append(String.join(" | ", rowData)).append("\n");
                    }
                }
                content.append("\n");
            }
        } catch (Exception e) {
            logger.error("Error extracting XLSX: {}", e.getMessage());
            throw new IOException("Failed to extract XLSX content: " + e.getMessage(), e);
        }
        return content.toString();
    }

    private String extractFromXls(byte[] fileData) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Workbook workbook = new HSSFWorkbook(new ByteArrayInputStream(fileData))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                content.append("【Sheet: ").append(sheet.getSheetName()).append("】\n");
                
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        rowData.add(getCellValueAsString(cell));
                    }
                    if (!rowData.isEmpty() && rowData.stream().anyMatch(v -> v != null && !v.isEmpty())) {
                        content.append(String.join(" | ", rowData)).append("\n");
                    }
                }
                content.append("\n");
            }
        } catch (Exception e) {
            logger.error("Error extracting XLS: {}", e.getMessage());
            throw new IOException("Failed to extract XLS content: " + e.getMessage(), e);
        }
        return content.toString();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                } else {
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value) && !Double.isInfinite(value)) {
                        yield String.valueOf((long) value);
                    }
                    yield String.valueOf(value);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getCellFormula();
                }
            }
            default -> "";
        };
    }

    private String extractFromPptx(byte[] fileData) throws IOException {
        StringBuilder content = new StringBuilder();
        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(fileData))) {
            int slideNum = 1;
            for (XSLFSlide slide : slideShow.getSlides()) {
                content.append("【Slide ").append(slideNum++).append("】\n");
                for (Object obj : slide.getShapes()) {
                    if (obj instanceof XSLFTextShape) {
                        XSLFTextShape ts = (XSLFTextShape) obj;
                        String text = ts.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\n");
                        }
                    }
                }
                content.append("\n");
            }
        } catch (Exception e) {
            logger.error("Error extracting PPTX: {}", e.getMessage());
            throw new IOException("Failed to extract PPTX content: " + e.getMessage(), e);
        }
        return content.toString();
    }

    private String extractFromOldPpt(byte[] fileData) throws IOException {
        StringBuilder content = new StringBuilder();
        try (HSLFSlideShow slideShow = new HSLFSlideShow(new ByteArrayInputStream(fileData))) {
            int slideNum = 1;
            for (HSLFSlide slide : slideShow.getSlides()) {
                content.append("【Slide ").append(slideNum++).append("】\n");
                for (Object obj : slide.getShapes()) {
                    if (obj instanceof HSLFTextShape) {
                        HSLFTextShape ts = (HSLFTextShape) obj;
                        String text = ts.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            content.append(text).append("\n");
                        }
                    }
                }
                content.append("\n");
            }
        } catch (Exception e) {
            logger.error("Error extracting PPT: {}", e.getMessage());
            throw new IOException("Failed to extract PPT content: " + e.getMessage(), e);
        }
        return content.toString();
    }

    public boolean isSupported(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        for (String e : WORD_EXTENSIONS) if (e.equals(ext)) return true;
        for (String e : EXCEL_EXTENSIONS) if (e.equals(ext)) return true;
        for (String e : PPT_EXTENSIONS) if (e.equals(ext)) return true;
        return false;
    }
}
