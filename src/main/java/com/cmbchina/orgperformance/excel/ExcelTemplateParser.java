package com.cmbchina.orgperformance.excel;

import com.cmbchina.orgperformance.entity.Indicator;
import com.cmbchina.orgperformance.entity.Institution;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ExcelTemplateParser {

    private static final String SHEET_TEMPLATE = "模版页";
    private static final String SHEET_INSTITUTION = "机构页";
    private static final String SHEET_DATA_COLLECTION = "数据收集页";
    private static final String SHEET_PARAMS = "参数页";

    public Map<String, Object> parseTemplate(InputStream inputStream) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            result.put("institutions", parseInstitutionSheet(workbook));
            result.put("indicators", parseTemplateSheet(workbook));
            result.put("dataCollection", parseDataCollectionSheet(workbook));
            result.put("params", parseParamsSheet(workbook));
        }
        return result;
    }

    public List<Institution> parseInstitutionSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_INSTITUTION);
        if (sheet == null) throw new RuntimeException("Missing sheet: " + SHEET_INSTITUTION);

        List<Institution> institutions = new ArrayList<>();
        int startRow = 0;
        
        // 检测表头行位置，跳过标题行
        // 如果第二行包含"机构名称"关键字，说明表头在第2行，数据从第3行开始
        for (int i = 0; i <= 2 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                String firstCell = getCellValueAsString(row.getCell(0));
                if ("机构名称".equals(firstCell)) {
                    startRow = i + 1;
                    break;
                }
            }
        }
        
        if (startRow == 0) startRow = 1;
        
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            
            // 检查是否是表头行
            String firstCell = getCellValueAsString(row.getCell(0));
            if ("机构名称".equals(firstCell) || firstCell.isEmpty()) continue;

            Institution inst = new Institution();
            inst.setOrgName(firstCell);
            inst.setOrgId(getCellValueAsString(row.getCell(1)));
            inst.setGroupName(getCellValueAsString(row.getCell(2)));
            String leader = getCellValueAsString(row.getCell(3));
            if (leader != null && leader.contains("/")) {
                String[] parts = leader.split("/");
                inst.setLeaderName(parts[0].trim());
                inst.setLeaderEmpNo(parts[1].trim());
            }
            institutions.add(inst);
        }
        return institutions;
    }

    public List<Indicator> parseTemplateSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_TEMPLATE);
        if (sheet == null) throw new RuntimeException("Missing sheet: " + SHEET_TEMPLATE);

        List<Indicator> indicators = new ArrayList<>();
        int startRow = 0;
        
        // 检测表头行位置，跳过标题和说明行
        // 如果第一行包含"维度"关键字，说明表头在第1行，数据从第2行开始
        // 如果第三行包含"维度"关键字，说明前两行是标题，数据从第4行开始
        for (int i = 0; i <= 3 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                String firstCell = getCellValueAsString(row.getCell(0));
                if ("维度".equals(firstCell)) {
                    startRow = i + 1; // 数据从下一行开始
                    break;
                }
            }
        }
        
        if (startRow == 0) startRow = 1;
        
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            
            // 检查是否是表头行
            String firstCell = getCellValueAsString(row.getCell(0));
            if ("维度".equals(firstCell) || firstCell.isEmpty()) continue;

            Indicator indicator = new Indicator();
            indicator.setDimension(firstCell);
            indicator.setCategory(getCellValueAsString(row.getCell(1)));
            indicator.setLevel1Name(getCellValueAsString(row.getCell(2)));
            indicator.setLevel2Name(getCellValueAsString(row.getCell(3)));
            indicator.setWeight(getCellValueAsBigDecimal(row.getCell(4)));
            indicator.setUnit(getCellValueAsString(row.getCell(5)));
            indicator.setAnnualTarget(getCellValueAsBigDecimal(row.getCell(6)));
            indicator.setProgressTarget(getCellValueAsBigDecimal(row.getCell(7)));
            indicator.setRowIndex(i);
            indicators.add(indicator);
        }
        return indicators;
    }

    public Map<String, Object> parseDataCollectionSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_DATA_COLLECTION);
        if (sheet == null) throw new RuntimeException("Missing sheet: " + SHEET_DATA_COLLECTION);

        Map<String, Object> result = new HashMap<>();
        List<String> indicators = new ArrayList<>();
        Map<String, String> collectors = new HashMap<>();
        Map<String, String> units = new HashMap<>();
        List<String> institutions = new ArrayList<>();

        Row row2 = sheet.getRow(1);
        Row row3 = sheet.getRow(2);
        Row row4 = sheet.getRow(3);

        for (int i = 1; i < getLastColumnNum(sheet); i++) {
            indicators.add(getCellValueAsString(row2.getCell(i)));
            collectors.put(indicators.get(indicators.size()-1), getCellValueAsString(row3.getCell(i)));
            units.put(indicators.get(indicators.size()-1), getCellValueAsString(row4.getCell(i)));
        }

        for (int i = 5; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            institutions.add(getCellValueAsString(row.getCell(0)));
        }

        result.put("indicators", indicators);
        result.put("collectors", collectors);
        result.put("units", units);
        result.put("institutions", institutions);
        return result;
    }

    public Map<String, String> parseParamsSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_PARAMS);
        if (sheet == null) return new HashMap<>();

        Map<String, String> params = new HashMap<>();
        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;
            String key = getCellValueAsString(row.getCell(0));
            String value = getCellValueAsString(row.getCell(1));
            if (key != null && !key.isEmpty()) {
                params.put(key, value);
            }
        }
        return params;
    }

    public void validateTemplate(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getSheet(SHEET_TEMPLATE) == null) {
                throw new RuntimeException("Missing required sheet: " + SHEET_TEMPLATE);
            }
            if (workbook.getSheet(SHEET_INSTITUTION) == null) {
                throw new RuntimeException("Missing required sheet: " + SHEET_INSTITUTION);
            }
            if (workbook.getSheet(SHEET_DATA_COLLECTION) == null) {
                throw new RuntimeException("Missing required sheet: " + SHEET_DATA_COLLECTION);
            }
            if (workbook.getSheet(SHEET_PARAMS) == null) {
                throw new RuntimeException("Missing required sheet: " + SHEET_PARAMS);
            }
        }
    }

    public ByteArrayInputStream generateReportFile(Institution institution, Integer year, Integer month,
                                                    Map<String, String> templateData, List<Map<String, Object>> indicatorData) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet templateSheet = workbook.createSheet("模版页");
            Sheet paramsSheet = workbook.createSheet("参数页");

            createParamsSheet(paramsSheet, institution, year, month);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report file", e);
        }
    }

    private void createParamsSheet(Sheet sheet, Institution institution, Integer year, Integer month) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("参数名称");
        headerCell1.setCellStyle(headerStyle);

        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("参数值");
        headerCell2.setCellStyle(headerStyle);

        String[][] params = {
                {"CURRENT_DATE", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))},
                {"CURRENT_YEAR", String.valueOf(year)},
                {"CURRENT_MONTH", String.format("%02d", month)},
                {"CURRENT_ORG", institution.getOrgName()},
                {"CURRENT_ORG_ID", institution.getOrgId()},
                {"TEMPLATE_VERSION", "V1.0"}
        };

        for (int i = 0; i < params.length; i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(params[i][0]);
            row.createCell(1).setCellValue(params[i][1]);
        }
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < getLastColumnNum(row.getSheet()); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getLastColumnNum(Sheet sheet) {
        int maxColumns = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getLastCellNum() > maxColumns) {
                maxColumns = (int) row.getLastCellNum();
            }
        }
        return maxColumns;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default: return "";
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        try {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } catch (Exception e) {
            // 尝试从字符串解析
            String str = getCellValueAsString(cell);
            if (str == null || str.isEmpty()) return null;
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException nfe) {
                // 无法解析为数字，返回null（如遇到"权重"等中文字符）
                return null;
            }
        }
    }
}
