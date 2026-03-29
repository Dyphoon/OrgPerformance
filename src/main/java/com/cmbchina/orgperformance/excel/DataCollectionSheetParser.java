package com.cmbchina.orgperformance.excel;

import com.cmbchina.orgperformance.entity.Indicator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * 解析Excel模板中的"数据收集页"
 * 用于获取收数指标和收数人分配信息
 */
@Component
public class DataCollectionSheetParser {

    public static class CollectionIndicator {
        private String indicatorName;      // 指标名称（如"日均存款余额"）
        private String unit;                 // 单位
        private String collectorName;       // 收数人名称
        private String collectorEmpNo;      // 收数人工号
        private int columnIndex;           // 在原表中的列索引

        // 关联的考核指标
        private String level1Name;
        private String level2Name;

        public String getIndicatorName() { return indicatorName; }
        public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getCollectorName() { return collectorName; }
        public void setCollectorName(String collectorName) { this.collectorName = collectorName; }
        public String getCollectorEmpNo() { return collectorEmpNo; }
        public void setCollectorEmpNo(String collectorEmpNo) { this.collectorEmpNo = collectorEmpNo; }
        public int getColumnIndex() { return columnIndex; }
        public void setColumnIndex(int columnIndex) { this.columnIndex = columnIndex; }
        public String getLevel1Name() { return level1Name; }
        public void setLevel1Name(String level1Name) { this.level1Name = level1Name; }
        public String getLevel2Name() { return level2Name; }
        public void setLevel2Name(String level2Name) { this.level2Name = level2Name; }

        public String getFullName() {
            return level1Name + (level2Name != null ? level2Name : "");
        }
    }

    public static class ParsedDataCollectionSheet {
        private List<String> institutionNames = new ArrayList<>();
        private Map<String, Integer> institutionRowMap = new HashMap<>();
        private List<CollectionIndicator> indicators = new ArrayList<>();

        public List<String> getInstitutionNames() { return institutionNames; }
        public Map<String, Integer> getInstitutionRowMap() { return institutionRowMap; }
        public List<CollectionIndicator> getIndicators() { return indicators; }
    }

    /**
     * 解析"数据收集页"
     * @param templateData Excel模板字节数组
     * @return 解析结果
     */
    public ParsedDataCollectionSheet parse(byte[] templateData) throws IOException {
        ParsedDataCollectionSheet result = new ParsedDataCollectionSheet();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(templateData))) {
            Sheet dataSheet = workbook.getSheet("数据收集页");
            if (dataSheet == null) {
                return result;
            }

            // 解析指标行（第2行，索引1）
            Row indicatorRow = dataSheet.getRow(1);
            // 解析收数人行（第3行，索引2）
            Row collectorRow = dataSheet.getRow(2);
            // 解析单位行（第4行，索引3）
            Row unitRow = dataSheet.getRow(3);

            if (indicatorRow == null || collectorRow == null) {
                return result;
            }

            // 遍历每个指标列（跳过第一列：机构名称）
            for (int col = 1; col < indicatorRow.getLastCellNum(); col++) {
                Cell indicatorCell = indicatorRow.getCell(col);
                Cell collectorCell = collectorRow.getCell(col);
                Cell unitCell = unitRow != null ? unitRow.getCell(col) : null;

                if (indicatorCell == null || getCellStringValue(indicatorCell).isEmpty()) {
                    continue;
                }

                String indicatorName = getCellStringValue(indicatorCell);
                String collectorInfo = getCellStringValue(collectorCell);
                String unit = unitCell != null ? getCellStringValue(unitCell) : "";

                // 解析收数人信息（格式："张三/EMP001"）
                String collectorName = "";
                String collectorEmpNo = "";
                if (collectorInfo != null && collectorInfo.contains("/")) {
                    String[] parts = collectorInfo.split("/");
                    collectorName = parts[0].trim();
                    collectorEmpNo = parts.length > 1 ? parts[1].trim() : "";
                } else {
                    collectorName = collectorInfo;
                }

                CollectionIndicator indicator = new CollectionIndicator();
                indicator.setIndicatorName(indicatorName);
                indicator.setUnit(unit);
                indicator.setCollectorName(collectorName);
                indicator.setCollectorEmpNo(collectorEmpNo);
                indicator.setColumnIndex(col);

                result.getIndicators().add(indicator);
            }

            // 解析机构名称（从第5行开始，索引4）
            for (int row = 4; row <= dataSheet.getLastRowNum(); row++) {
                Row dataRow = dataSheet.getRow(row);
                if (dataRow == null) continue;

                Cell nameCell = dataRow.getCell(0);
                String instName = getCellStringValue(nameCell);
                if (instName != null && !instName.isEmpty()) {
                    result.getInstitutionNames().add(instName);
                    result.getInstitutionRowMap().put(instName, row);
                }
            }
        }

        return result;
    }

    /**
     * 根据收数人名称获取其负责的指标列表
     */
    public List<CollectionIndicator> getIndicatorsByCollector(ParsedDataCollectionSheet sheet, String collectorName) {
        List<CollectionIndicator> result = new ArrayList<>();
        for (CollectionIndicator ind : sheet.getIndicators()) {
            if (collectorName.equals(ind.getCollectorName())) {
                result.add(ind);
            }
        }
        return result;
    }

    /**
     * 匹配数据收集指标与考核指标
     */
    public void matchWithAssessmentIndicators(ParsedDataCollectionSheet sheet, List<Indicator> assessmentIndicators) {
        for (CollectionIndicator colInd : sheet.getIndicators()) {
            // 尝试匹配：indicatorName 匹配 level2Name
            for (Indicator assInd : assessmentIndicators) {
                String level2Name = assInd.getLevel2Name() != null ? assInd.getLevel2Name() : "";
                // 精确匹配
                if (colInd.getIndicatorName().equals(level2Name)) {
                    colInd.setLevel1Name(assInd.getLevel1Name());
                    colInd.setLevel2Name(level2Name);
                    break;
                }
                // 或者匹配indicatorName
                if (colInd.getIndicatorName().equals(assInd.getLevel1Name())) {
                    colInd.setLevel1Name(assInd.getLevel1Name());
                    colInd.setLevel2Name(level2Name);
                    break;
                }
            }
            // 如果没有匹配，尝试模糊匹配
            if (colInd.getLevel1Name() == null) {
                for (Indicator assInd : assessmentIndicators) {
                    String fullName = assInd.getLevel1Name() + (assInd.getLevel2Name() != null ? assInd.getLevel2Name() : "");
                    if (fullName.contains(colInd.getIndicatorName()) || colInd.getIndicatorName().contains(fullName)) {
                        colInd.setLevel1Name(assInd.getLevel1Name());
                        colInd.setLevel2Name(assInd.getLevel2Name());
                        break;
                    }
                }
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
}
