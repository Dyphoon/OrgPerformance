package com.cmbchina.orgperformance.excel;

import com.cmbchina.orgperformance.entity.Indicator;
import com.cmbchina.orgperformance.entity.MonthlyIndicatorData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Component
public class ExcelDataReader {

    private static final String SHEET_TEMPLATE = "模版页";

    public List<MonthlyIndicatorData> readIndicatorDataFromFile(InputStream fileStream, Long monitoringId,
                                                                   Long institutionId, Long indicatorId,
                                                                   String fileKey) throws IOException {
        List<MonthlyIndicatorData> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(fileStream)) {
            Sheet sheet = workbook.getSheet(SHEET_TEMPLATE);
            if (sheet == null) return dataList;

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                MonthlyIndicatorData data = new MonthlyIndicatorData();
                data.setMonitoringId(monitoringId);
                data.setInstitutionId(institutionId);
                data.setIndicatorId(indicatorId);
                data.setActualValue(getNumericCellValue(row.getCell(8)));
                data.setAnnualCompletionRate(getNumericCellValue(row.getCell(9)));
                data.setProgressCompletionRate(getNumericCellValue(row.getCell(10)));
                data.setScore100(getNumericCellValue(row.getCell(11)));
                data.setScoreWeighted(getNumericCellValue(row.getCell(12)));
                data.setScoreCategory(getNumericCellValue(row.getCell(13)));
                data.setScoreDimension(getNumericCellValue(row.getCell(14)));
                data.setTotalScore(getNumericCellValue(row.getCell(15)));
                data.setFileKey(fileKey);

                dataList.add(data);
            }
        }

        return dataList;
    }

    public Map<String, Map<String, BigDecimal>> readDataCollectionFromFile(InputStream fileStream) throws IOException {
        Map<String, Map<String, BigDecimal>> result = new HashMap<>();

        try (Workbook workbook = new XSSFWorkbook(fileStream)) {
            Sheet sheet = workbook.getSheet("数据收集页");
            if (sheet == null) return result;

            List<String> indicators = new ArrayList<>();
            Row headerRow = sheet.getRow(1);
            for (int i = 1; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    indicators.add(getCellStringValue(cell));
                }
            }

            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String institutionName = getCellStringValue(row.getCell(0));
                if (institutionName == null || institutionName.isEmpty()) continue;

                Map<String, BigDecimal> institutionData = new HashMap<>();
                for (int j = 0; j < indicators.size(); j++) {
                    Cell dataCell = row.getCell(j + 1);
                    if (dataCell != null) {
                        institutionData.put(indicators.get(j), getNumericCellValue(dataCell));
                    }
                }
                result.put(institutionName, institutionData);
            }
        }

        return result;
    }

    public Map<String, BigDecimal> readIndicatorDataFromDataCollectionSheet(InputStream fileStream, String institutionName) throws IOException {
        Map<String, BigDecimal> result = new HashMap<>();

        try (Workbook workbook = new XSSFWorkbook(fileStream)) {
            Sheet sheet = workbook.getSheet("数据收集页");
            if (sheet == null) return result;

            List<String> indicators = new ArrayList<>();
            Row headerRow = sheet.getRow(1);
            if (headerRow == null) return result;

            for (int i = 1; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    indicators.add(getCellStringValue(cell));
                }
            }

            for (int i = 4; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String instName = getCellStringValue(row.getCell(0));
                if (institutionName != null && institutionName.equals(instName)) {
                    for (int j = 0; j < indicators.size(); j++) {
                        Cell dataCell = row.getCell(j + 1);
                        if (dataCell != null) {
                            BigDecimal value = getNumericCellValue(dataCell);
                            if (value != null) {
                                result.put(indicators.get(j), value);
                            }
                        }
                    }
                    break;
                }
            }
        }

        return result;
    }

    public Map<String, Object> readTemplateData(InputStream fileStream) throws IOException {
        Map<String, Object> result = new HashMap<>();

        try (Workbook workbook = new XSSFWorkbook(fileStream)) {
            Sheet sheet = workbook.getSheet(SHEET_TEMPLATE);
            if (sheet == null) return result;

            List<Map<String, Object>> indicators = new ArrayList<>();
            // Template sheet: dynamically find header row (contains "维度")
            int dataStartRow = 3; // default fallback
            for (int i = 0; i <= 5 && i <= sheet.getLastRowNum(); i++) {
                Row headerRow = sheet.getRow(i);
                if (headerRow != null && "维度".equals(getCellStringValue(headerRow.getCell(0)))) {
                    dataStartRow = i + 1;
                    break;
                }
            }

            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String level1 = getCellStringValue(row.getCell(2));
                String level2 = getCellStringValue(row.getCell(3));
                BigDecimal actualVal = getNumericCellValue(row.getCell(8));
                BigDecimal annualComp = getNumericCellValue(row.getCell(9));
                BigDecimal score = getNumericCellValue(row.getCell(11));

                Map<String, Object> indicator = new HashMap<>();
                indicator.put("dimension", getCellStringValue(row.getCell(0)));
                indicator.put("category", getCellStringValue(row.getCell(1)));
                indicator.put("level1Name", level1);
                indicator.put("level2Name", level2);
                indicator.put("weight", getNumericCellValue(row.getCell(4)));
                indicator.put("unit", getCellStringValue(row.getCell(5)));
                indicator.put("annualTarget", getNumericCellValue(row.getCell(6)));
                indicator.put("progressTarget", getNumericCellValue(row.getCell(7)));
                indicator.put("actualValue", actualVal);
                indicator.put("annualCompletionRate", annualComp);
                indicator.put("progressCompletionRate", getNumericCellValue(row.getCell(10)));
                indicator.put("score100", score);
                indicator.put("scoreWeighted", getNumericCellValue(row.getCell(12)));
                indicator.put("scoreCategory", getNumericCellValue(row.getCell(13)));
                indicator.put("scoreDimension", getNumericCellValue(row.getCell(14)));
                indicator.put("totalScore", getNumericCellValue(row.getCell(15)));
                indicators.add(indicator);
            }
            result.put("indicators", indicators);
        }

        return result;
    }
    

    private BigDecimal getNumericCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } catch (Exception e) {
            try {
                return new BigDecimal(cell.getStringCellValue());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String getCellStringValue(Cell cell) {
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

    public byte[] writeIndicatorDataToExcel(List<Map<String, Object>> indicators, Map<String, Object> metadata) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("模版页");

            String[] headers = {"维度", "类别", "一级指标", "二级指标", "权重", "单位",
                    "全年目标", "进度目标", "实际值", "全年完成率", "进度完成率",
                    "指标百分制得分", "指标权重得分", "类别得分", "维度得分", "总得分"};

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            for (int i = 0; i < indicators.size(); i++) {
                Map<String, Object> ind = indicators.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue((String) ind.get("dimension"));
                row.createCell(1).setCellValue((String) ind.get("category"));
                row.createCell(2).setCellValue((String) ind.get("level1Name"));
                row.createCell(3).setCellValue((String) ind.get("level2Name"));
                setNumericCell(row.createCell(4), ind.get("weight"));
                row.createCell(5).setCellValue((String) ind.get("unit"));
                setNumericCell(row.createCell(6), ind.get("annualTarget"));
                setNumericCell(row.createCell(7), ind.get("progressTarget"));
                setNumericCell(row.createCell(8), ind.get("actualValue"));
                setNumericCell(row.createCell(9), ind.get("annualCompletionRate"));
                setNumericCell(row.createCell(10), ind.get("progressCompletionRate"));
                setNumericCell(row.createCell(11), ind.get("score100"));
                setNumericCell(row.createCell(12), ind.get("scoreWeighted"));
                setNumericCell(row.createCell(13), ind.get("scoreCategory"));
                setNumericCell(row.createCell(14), ind.get("scoreDimension"));
                setNumericCell(row.createCell(15), ind.get("totalScore"));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write Excel", e);
        }
    }

    private void setNumericCell(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            try {
                cell.setCellValue(new BigDecimal(value.toString()).doubleValue());
            } catch (Exception e) {
                cell.setCellValue(value.toString());
            }
        }
    }
}
