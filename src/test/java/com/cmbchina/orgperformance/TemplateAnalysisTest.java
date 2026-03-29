package com.cmbchina.orgperformance;

import com.cmbchina.orgperformance.excel.ExcelDataReader;
import com.cmbchina.orgperformance.excel.ExcelTemplateParser;
import com.cmbchina.orgperformance.minio.MinioService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@SpringBootTest
class TemplateAnalysisTest {

    @Autowired
    private MinioService minioService;

    @Test
    void analyzeTemplateSheets() throws Exception {
        // Download template for system 11
        String templateKey = "templates/11/template_1774682095485.xlsx";
        byte[] templateData = minioService.downloadAsBytes(
                minioService.getBucketTemplates(), templateKey);

        System.out.println("=== Template Analysis ===");
        System.out.println("Template size: " + templateData.length + " bytes");

        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(templateData))) {
            System.out.println("\nSheets in workbook:");
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("  " + (i+1) + ". " + sheet.getSheetName() + " (" + sheet.getLastRowNum() + " rows)");
            }

            // Analyze 模版页
            System.out.println("\n=== 模版页 (Assessment Template) ===");
            Sheet templateSheet = workbook.getSheet("模版页");
            if (templateSheet != null) {
                System.out.println("Headers (Row 3):");
                Row headerRow = templateSheet.getRow(2); // Row index 2 = 3rd row
                if (headerRow != null) {
                    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                        Cell cell = headerRow.getCell(i);
                        System.out.print("  Col " + i + ": " + (cell != null ? cell.getStringCellValue() : ""));
                    }
                }
                System.out.println("\nData rows:");
                for (int i = 3; i <= templateSheet.getLastRowNum(); i++) {
                    Row row = templateSheet.getRow(i);
                    if (row != null) {
                        StringBuilder sb = new StringBuilder("  Row " + (i+1) + ": ");
                        for (int j = 0; j < 5 && j < row.getLastCellNum(); j++) {
                            Cell cell = row.getCell(j);
                            sb.append(getCellValue(cell)).append(" | ");
                        }
                        System.out.println(sb);
                    }
                }
            }

            // Analyze 数据收集页
            System.out.println("\n=== 数据收集页 (Data Collection) ===");
            Sheet dataSheet = workbook.getSheet("数据收集页");
            if (dataSheet != null) {
                System.out.println("Row 2 (indicators):");
                Row row2 = dataSheet.getRow(1);
                if (row2 != null) {
                    for (int i = 0; i < row2.getLastCellNum(); i++) {
                        Cell cell = row2.getCell(i);
                        System.out.print("  Col " + i + ": " + (cell != null ? cell.getStringCellValue() : ""));
                    }
                }
                System.out.println("\nRow 3 (collectors):");
                Row row3 = dataSheet.getRow(2);
                if (row3 != null) {
                    for (int i = 0; i < row3.getLastCellNum(); i++) {
                        Cell cell = row3.getCell(i);
                        System.out.print("  Col " + i + ": " + (cell != null ? cell.getStringCellValue() : ""));
                    }
                }
                System.out.println("\nRow 4 (units):");
                Row row4 = dataSheet.getRow(3);
                if (row4 != null) {
                    for (int i = 0; i < row4.getLastCellNum(); i++) {
                        Cell cell = row4.getCell(i);
                        System.out.print("  Col " + i + ": " + (cell != null ? cell.getStringCellValue() : ""));
                    }
                }
                System.out.println("\nData rows (institutions):");
                for (int i = 4; i <= dataSheet.getLastRowNum(); i++) {
                    Row row = dataSheet.getRow(i);
                    if (row != null) {
                        StringBuilder sb = new StringBuilder("  Row " + (i+1) + ": ");
                        for (int j = 0; j < row.getLastCellNum(); j++) {
                            Cell cell = row.getCell(j);
                            sb.append(getCellValue(cell)).append(" | ");
                        }
                        System.out.println(sb);
                    }
                }
            } else {
                System.out.println("Sheet '数据收集页' not found!");
            }
        }

        System.out.println("\n=== Test Complete ===");
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return "";
        }
    }
}
