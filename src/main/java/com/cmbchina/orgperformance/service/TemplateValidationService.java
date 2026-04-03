package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.entity.Indicator;
import com.cmbchina.orgperformance.entity.Institution;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * 绩效考核体系模板校验服务
 * 提供详细的模板格式校验，帮助用户修正模板问题
 */
@Service
public class TemplateValidationService {

    private static final String SHEET_TEMPLATE = "模版页";
    private static final String SHEET_INSTITUTION = "机构页";
    private static final String SHEET_DATA_COLLECTION = "数据收集页";
    private static final String SHEET_PARAMS = "参数页";

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private boolean valid = true;
        private final List<ValidationError> errors = new ArrayList<>();
        private final List<ValidationWarning> warnings = new ArrayList<>();
        private int institutionCount = 0;
        private int indicatorCount = 0;
        private final Map<String, Object> preview = new LinkedHashMap<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<ValidationError> getErrors() { return errors; }
        public List<ValidationWarning> getWarnings() { return warnings; }
        public int getInstitutionCount() { return institutionCount; }
        public int getIndicatorCount() { return indicatorCount; }
        public Map<String, Object> getPreview() { return preview; }

        public void addError(String code, String message, String sheet, Integer row, Integer column) {
            errors.add(new ValidationError(code, message, sheet, row, column));
            valid = false;
        }

        public void addError(String code, String message, String sheet) {
            addError(code, message, sheet, null, null);
        }

        public void addWarning(String code, String message, String sheet, Integer row, Integer column) {
            warnings.add(new ValidationWarning(code, message, sheet, row, column));
        }

        public void addWarning(String code, String message, String sheet) {
            addWarning(code, message, sheet, null, null);
        }
    }

    public static class ValidationError {
        private final String code;
        private final String message;
        private final String sheet;
        private final Integer row;
        private final Integer column;

        public ValidationError(String code, String message, String sheet, Integer row, Integer column) {
            this.code = code;
            this.message = message;
            this.sheet = sheet;
            this.row = row;
            this.column = column;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getSheet() { return sheet; }
        public Integer getRow() { return row; }
        public Integer getColumn() { return column; }

        public String toUserMessage() {
            StringBuilder sb = new StringBuilder();
            if (sheet != null) {
                sb.append("[").append(sheet).append("] ");
            }
            if (row != null) {
                sb.append("第").append(row).append("行");
                if (column != null) {
                    sb.append("第").append(column).append("列");
                }
                sb.append(": ");
            }
            sb.append(message);
            return sb.toString();
        }
    }

    public static class ValidationWarning {
        private final String code;
        private final String message;
        private final String sheet;
        private final Integer row;
        private final Integer column;

        public ValidationWarning(String code, String message, String sheet, Integer row, Integer column) {
            this.code = code;
            this.message = message;
            this.sheet = sheet;
            this.row = row;
            this.column = column;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getSheet() { return sheet; }
        public Integer getRow() { return row; }
        public Integer getColumn() { return column; }

        public String toUserMessage() {
            StringBuilder sb = new StringBuilder();
            if (sheet != null) {
                sb.append("[").append(sheet).append("] ");
            }
            if (row != null) {
                sb.append("第").append(row).append("行");
                if (column != null) {
                    sb.append("第").append(column).append("列");
                }
                sb.append(": ");
            }
            sb.append(message);
            return sb.toString();
        }
    }

    /**
     * 校验模板文件
     */
    public ValidationResult validate(InputStream inputStream) throws IOException {
        ValidationResult result = new ValidationResult();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            // 1. 校验工作表结构
            validateSheets(workbook, result);

            // 如果缺少必需工作表，直接返回
            if (!result.isValid()) {
                return result;
            }

            // 2. 校验模版页
            validateTemplateSheet(workbook, result);

            // 3. 校验机构页
            validateInstitutionSheet(workbook, result);

            // 4. 校验数据收集页
            validateDataCollectionSheet(workbook, result);

            // 5. 校验参数页
            validateParamsSheet(workbook, result);

            // 6. 生成预览数据
            generatePreview(workbook, result);
        }

        return result;
    }

    /**
     * 校验是否包含所有必需的工作表
     */
    private void validateSheets(Workbook workbook, ValidationResult result) {
        List<String> requiredSheets = Arrays.asList(SHEET_TEMPLATE, SHEET_INSTITUTION, SHEET_DATA_COLLECTION, SHEET_PARAMS);
        List<String> actualSheets = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            actualSheets.add(workbook.getSheetAt(i).getSheetName());
        }

        for (String required : requiredSheets) {
            if (!actualSheets.contains(required)) {
                result.addError("E001", "缺少必需的工作表【" + required + "】", required);
            }
        }
    }

    /**
     * 校验模版页（指标定义）
     */
    private void validateTemplateSheet(Workbook workbook, ValidationResult result) {
        Sheet sheet = workbook.getSheet(SHEET_TEMPLATE);
        if (sheet == null) return;

        // 查找表头行
        int headerRow = -1;
        for (int i = 0; i <= 5 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && "维度".equals(getCellValueAsString(row.getCell(0)))) {
                headerRow = i;
                break;
            }
        }

        if (headerRow == -1) {
            result.addError("E101", "未找到表头行，请确保工作表包含【维度】作为第一列表头", SHEET_TEMPLATE);
            return;
        }

        // 校验表头列
        Row header = sheet.getRow(headerRow);
        String[] requiredHeaders = {"维度", "类别", "一级指标", "二级指标", "权重", "单位"};
        for (int i = 0; i < requiredHeaders.length; i++) {
            Cell cell = header.getCell(i);
            if (cell == null || !requiredHeaders[i].equals(getCellValueAsString(cell).trim())) {
                result.addError("E102", "第" + (headerRow + 1) + "行第" + (i + 1) + "列应为【" + requiredHeaders[i] + "】", SHEET_TEMPLATE, headerRow + 1, i + 1);
            }
        }

        // 校验数据行
        Set<String> seenIndicators = new HashSet<>();
        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row, 6)) continue;

            // 检查是否是表头行
            String firstCell = getCellValueAsString(row.getCell(0));
            if ("维度".equals(firstCell)) continue;

            int rowNum = i + 1;

            // 维度（可选）
            String dimension = getCellValueAsString(row.getCell(0));
            if (dimension.isEmpty()) {
                result.addWarning("W101", "维度为空", SHEET_TEMPLATE, rowNum, 1);
            }

            // 类别（可选）
            String category = getCellValueAsString(row.getCell(1));
            if (category.isEmpty()) {
                result.addWarning("W102", "类别为空", SHEET_TEMPLATE, rowNum, 2);
            }

            // 一级指标（必填）
            String level1 = getCellValueAsString(row.getCell(2));
            if (level1.isEmpty()) {
                result.addError("E103", "一级指标不能为空", SHEET_TEMPLATE, rowNum, 3);
            }

            // 二级指标（必填）
            String level2 = getCellValueAsString(row.getCell(3));
            if (level2.isEmpty()) {
                result.addError("E104", "二级指标不能为空", SHEET_TEMPLATE, rowNum, 4);
            }

            // 检查重复指标
            String indicatorKey = dimension + "|" + category + "|" + level1 + "|" + level2;
            if (!level1.isEmpty() && !level2.isEmpty()) {
                if (seenIndicators.contains(indicatorKey)) {
                    result.addWarning("W103", "指标重复: " + level1 + " - " + level2, SHEET_TEMPLATE, rowNum, 3);
                }
                seenIndicators.add(indicatorKey);
            }

            // 权重（必填）
            Cell weightCell = row.getCell(4);
            if (weightCell == null || getCellValueAsString(weightCell).trim().isEmpty()) {
                result.addError("E105", "权重不能为空", SHEET_TEMPLATE, rowNum, 5);
            } else {
                BigDecimal weight = getCellValueAsBigDecimal(weightCell);
                if (weight == null) {
                    result.addError("E106", "权重必须是数字", SHEET_TEMPLATE, rowNum, 5);
                } else if (weight.compareTo(BigDecimal.ZERO) < 0) {
                    result.addError("E107", "权重不能为负数", SHEET_TEMPLATE, rowNum, 5);
                } else if (weight.compareTo(BigDecimal.ONE) > 1) {
                    result.addWarning("W104", "权重超过1（" + weight + "），可能影响总分计算", SHEET_TEMPLATE, rowNum, 5);
                }
            }

            // 单位（必填）
            String unit = getCellValueAsString(row.getCell(5));
            if (unit.isEmpty()) {
                result.addError("E108", "单位不能为空", SHEET_TEMPLATE, rowNum, 6);
            }
        }

        // 检查是否有指标数据
        if (seenIndicators.isEmpty()) {
            result.addError("E109", "模版页中没有有效的指标数据", SHEET_TEMPLATE);
        }
    }

    /**
     * 校验机构页
     */
    private void validateInstitutionSheet(Workbook workbook, ValidationResult result) {
        Sheet sheet = workbook.getSheet(SHEET_INSTITUTION);
        if (sheet == null) return;

        // 查找表头行
        int headerRow = -1;
        for (int i = 0; i <= 5 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && "机构名称".equals(getCellValueAsString(row.getCell(0)))) {
                headerRow = i;
                break;
            }
        }

        if (headerRow == -1) {
            result.addError("E201", "未找到表头行，请确保工作表包含【机构名称】作为第一列表头", SHEET_INSTITUTION);
            return;
        }

        // 校验表头列
        Row header = sheet.getRow(headerRow);
        String[] requiredHeaders = {"机构名称", "机构ID", "分组名称", "机构负责人"};
        for (int i = 0; i < requiredHeaders.length; i++) {
            Cell cell = header.getCell(i);
            if (cell == null || !requiredHeaders[i].equals(getCellValueAsString(cell).trim())) {
                result.addError("E202", "第" + (headerRow + 1) + "行第" + (i + 1) + "列应为【" + requiredHeaders[i] + "】", SHEET_INSTITUTION, headerRow + 1, i + 1);
            }
        }

        // 校验数据行
        Set<String> seenOrgIds = new HashSet<>();
        Set<String> seenOrgNames = new HashSet<>();
        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row, 4)) continue;

            String firstCell = getCellValueAsString(row.getCell(0));
            if ("机构名称".equals(firstCell) || firstCell.isEmpty()) continue;

            int rowNum = i + 1;

            // 机构名称（必填）
            String orgName = getCellValueAsString(row.getCell(0));
            if (orgName.isEmpty()) {
                result.addError("E203", "机构名称不能为空", SHEET_INSTITUTION, rowNum, 1);
            } else {
                if (seenOrgNames.contains(orgName)) {
                    result.addWarning("W201", "机构名称重复: " + orgName, SHEET_INSTITUTION, rowNum, 1);
                }
                seenOrgNames.add(orgName);
            }

            // 机构ID（必填）
            String orgId = getCellValueAsString(row.getCell(1));
            if (orgId.isEmpty()) {
                result.addError("E204", "机构ID不能为空", SHEET_INSTITUTION, rowNum, 2);
            } else {
                if (seenOrgIds.contains(orgId)) {
                    result.addError("E205", "机构ID重复: " + orgId, SHEET_INSTITUTION, rowNum, 2);
                }
                seenOrgIds.add(orgId);
            }

            // 分组名称（可选）
            String groupName = getCellValueAsString(row.getCell(2));
            if (groupName.isEmpty()) {
                result.addWarning("W202", "分组名称为空", SHEET_INSTITUTION, rowNum, 3);
            }

            // 机构负责人（可选，但格式必须正确）
            String leader = getCellValueAsString(row.getCell(3));
            if (!leader.isEmpty() && !leader.contains("/")) {
                result.addWarning("W203", "机构负责人格式应为【姓名/工号】，如：张三/EMP001", SHEET_INSTITUTION, rowNum, 4);
            }
        }

        // 检查是否有机构数据
        if (seenOrgNames.isEmpty()) {
            result.addError("E206", "机构页中没有有效的机构数据", SHEET_INSTITUTION);
        }
    }

    /**
     * 校验数据收集页
     */
    private void validateDataCollectionSheet(Workbook workbook, ValidationResult result) {
        Sheet sheet = workbook.getSheet(SHEET_DATA_COLLECTION);
        if (sheet == null) return;

        // 校验第2行（指标名称行）
        Row row2 = sheet.getRow(1);
        if (row2 == null) {
            result.addError("E301", "第2行（指标名称行）不存在", SHEET_DATA_COLLECTION);
            return;
        }

        Cell cell1 = row2.getCell(0);
        if (cell1 == null || !"机构名称".equals(getCellValueAsString(cell1).trim())) {
            result.addError("E302", "第2行第1列应为【机构名称】", SHEET_DATA_COLLECTION, 2, 1);
        }

        // 校验第3行（收数人行）
        Row row3 = sheet.getRow(2);
        if (row3 == null) {
            result.addError("E303", "第3行（收数人行）不存在", SHEET_DATA_COLLECTION);
        } else {
            Cell cell1_3 = row3.getCell(0);
            if (cell1_3 == null || !"收数人".equals(getCellValueAsString(cell1_3).trim())) {
                result.addError("E304", "第3行第1列应为【收数人】", SHEET_DATA_COLLECTION, 3, 1);
            }
        }

        // 校验第4行（指标单位行）
        Row row4 = sheet.getRow(3);
        if (row4 == null) {
            result.addError("E305", "第4行（指标单位行）不存在", SHEET_DATA_COLLECTION);
        } else {
            Cell cell1_4 = row4.getCell(0);
            if (cell1_4 == null || !"指标单位".equals(getCellValueAsString(cell1_4).trim())) {
                result.addError("E306", "第4行第1列应为【指标单位】", SHEET_DATA_COLLECTION, 4, 1);
            }
        }

        // 检查是否有数据列（指标）
        boolean hasIndicators = false;
        for (int i = 1; i < getLastColumnNum(sheet); i++) {
            Cell cell = row2 != null ? row2.getCell(i) : null;
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                hasIndicators = true;
                break;
            }
        }
        if (!hasIndicators) {
            result.addWarning("W301", "数据收集页中没有配置任何指标", SHEET_DATA_COLLECTION);
        }
    }

    /**
     * 校验参数页
     */
    private void validateParamsSheet(Workbook workbook, ValidationResult result) {
        Sheet sheet = workbook.getSheet(SHEET_PARAMS);
        if (sheet == null) return;

        // 查找表头行
        int headerRow = -1;
        for (int i = 0; i <= 5 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && "参数名称".equals(getCellValueAsString(row.getCell(0)))) {
                headerRow = i;
                break;
            }
        }

        if (headerRow == -1) {
            result.addWarning("W401", "未找到参数页表头【参数名称】", SHEET_PARAMS);
        }

        // 必需的参数
        List<String> requiredParams = Arrays.asList("CURRENT_DATE", "CURRENT_YEAR", "CURRENT_MONTH", "CURRENT_ORG", "CURRENT_ORG_ID");
        Set<String> foundParams = new HashSet<>();

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Cell keyCell = row.getCell(0);
            if (keyCell == null) continue;
            String key = getCellValueAsString(keyCell).trim();
            if (!key.isEmpty()) {
                foundParams.add(key);
            }
        }

        for (String required : requiredParams) {
            if (!foundParams.contains(required)) {
                result.addWarning("W402", "建议包含参数【" + required + "】", SHEET_PARAMS);
            }
        }
    }

    /**
     * 生成预览数据
     */
    private void generatePreview(Workbook workbook, ValidationResult result) {
        // 解析机构预览
        List<Institution> institutions = parseInstitutionSheet(workbook);
        result.institutionCount = institutions.size();
        List<Map<String, Object>> institutionPreview = institutions.stream().limit(5).map(inst -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orgName", inst.getOrgName());
            map.put("orgId", inst.getOrgId());
            map.put("groupName", inst.getGroupName());
            map.put("leader", inst.getLeaderName() + (inst.getLeaderEmpNo() != null ? "/" + inst.getLeaderEmpNo() : ""));
            return map;
        }).toList();
        result.preview.put("institutionPreview", institutionPreview);

        // 解析指标预览
        List<Indicator> indicators = parseTemplateSheet(workbook);
        result.indicatorCount = indicators.size();
        List<Map<String, Object>> indicatorPreview = indicators.stream().limit(5).map(ind -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("dimension", ind.getDimension());
            map.put("category", ind.getCategory());
            map.put("level1Name", ind.getLevel1Name());
            map.put("level2Name", ind.getLevel2Name());
            map.put("weight", ind.getWeight());
            map.put("unit", ind.getUnit());
            return map;
        }).toList();
        result.preview.put("indicatorPreview", indicatorPreview);
    }

    /**
     * 解析机构页
     */
    private List<Institution> parseInstitutionSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_INSTITUTION);
        if (sheet == null) return Collections.emptyList();

        List<Institution> institutions = new ArrayList<>();
        int startRow = 1;

        for (int i = 0; i <= 5 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && "机构名称".equals(getCellValueAsString(row.getCell(0)))) {
                startRow = i + 1;
                break;
            }
        }

        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row, 4)) continue;
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

    /**
     * 解析模版页
     */
    private List<Indicator> parseTemplateSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet(SHEET_TEMPLATE);
        if (sheet == null) return Collections.emptyList();

        List<Indicator> indicators = new ArrayList<>();
        int startRow = 1;

        for (int i = 0; i <= 5 && i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && "维度".equals(getCellValueAsString(row.getCell(0)))) {
                startRow = i + 1;
                break;
            }
        }

        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row, 6)) continue;
            String firstCell = getCellValueAsString(row.getCell(0));
            if ("维度".equals(firstCell)) continue;

            Indicator indicator = new Indicator();
            indicator.setDimension(firstCell);
            indicator.setCategory(getCellValueAsString(row.getCell(1)));
            indicator.setLevel1Name(getCellValueAsString(row.getCell(2)));
            indicator.setLevel2Name(getCellValueAsString(row.getCell(3)));
            indicator.setWeight(getCellValueAsBigDecimal(row.getCell(4)));
            indicator.setUnit(getCellValueAsString(row.getCell(5)));
            indicators.add(indicator);
        }
        return indicators;
    }

    private boolean isEmptyRow(Row row, int maxCols) {
        for (int i = 0; i < maxCols; i++) {
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
            String str = getCellValueAsString(cell);
            if (str == null || str.isEmpty()) return null;
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

    /**
     * 生成用户友好的错误摘要
     */
    public String generateErrorSummary(ValidationResult result) {
        if (result.isValid() && result.getWarnings().isEmpty()) {
            return "模板校验通过，未发现问题";
        }

        StringBuilder sb = new StringBuilder();

        if (!result.isValid()) {
            sb.append("❌ 模板存在以下错误，请修正后重新上传：\n\n");
            for (ValidationError error : result.getErrors()) {
                sb.append("• ").append(error.toUserMessage()).append("\n");
            }
        }

        if (!result.getWarnings().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("⚠️  警告（不影响创建，但建议修正）：\n\n");
            for (ValidationWarning warning : result.getWarnings()) {
                sb.append("• ").append(warning.toUserMessage()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 生成详细的校验报告（用于返回给前端/MCP）
     */
    public Map<String, Object> generateDetailedReport(ValidationResult result) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("valid", result.isValid());

        // 错误列表
        List<Map<String, Object>> errors = new ArrayList<>();
        for (ValidationError error : result.getErrors()) {
            Map<String, Object> errMap = new LinkedHashMap<>();
            errMap.put("code", error.getCode());
            errMap.put("message", error.toUserMessage());
            errMap.put("sheet", error.getSheet());
            errMap.put("row", error.getRow());
            errMap.put("column", error.getColumn());
            errors.add(errMap);
        }
        report.put("errors", errors);

        // 警告列表
        List<Map<String, Object>> warnings = new ArrayList<>();
        for (ValidationWarning warning : result.getWarnings()) {
            Map<String, Object> warnMap = new LinkedHashMap<>();
            warnMap.put("code", warning.getCode());
            warnMap.put("message", warning.toUserMessage());
            warnMap.put("sheet", warning.getSheet());
            warnMap.put("row", warning.getRow());
            warnMap.put("column", warning.getColumn());
            warnings.add(warnMap);
        }
        report.put("warnings", warnings);

        // 统计数据
        report.put("institutionCount", result.institutionCount);
        report.put("indicatorCount", result.indicatorCount);
        report.put("errorCount", result.getErrors().size());
        report.put("warningCount", result.getWarnings().size());

        // 摘要
        report.put("summary", generateErrorSummary(result));

        // 预览数据
        report.putAll(result.getPreview());

        return report;
    }
}
