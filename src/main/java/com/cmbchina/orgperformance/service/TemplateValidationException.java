package com.cmbchina.orgperformance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模板校验异常
 * 当模板格式不符合要求时抛出此异常，包含详细的校验结果
 */
public class TemplateValidationException extends RuntimeException {

    private final boolean valid;
    private final List<ValidationErrorInfo> errors;
    private final List<ValidationWarningInfo> warnings;
    private final int institutionCount;
    private final int indicatorCount;

    public static class ValidationErrorInfo {
        private final String code;
        private final String message;
        private final String sheet;
        private final Integer row;
        private final Integer column;

        public ValidationErrorInfo(String code, String message, String sheet, Integer row, Integer column) {
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
            if (sheet != null) sb.append("[").append(sheet).append("] ");
            if (row != null) {
                sb.append("第").append(row).append("行");
                if (column != null) sb.append("第").append(column).append("列");
                sb.append(": ");
            }
            sb.append(message);
            return sb.toString();
        }
    }

    public static class ValidationWarningInfo {
        private final String code;
        private final String message;
        private final String sheet;
        private final Integer row;
        private final Integer column;

        public ValidationWarningInfo(String code, String message, String sheet, Integer row, Integer column) {
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
            if (sheet != null) sb.append("[").append(sheet).append("] ");
            if (row != null) {
                sb.append("第").append(row).append("行");
                if (column != null) sb.append("第").append(column).append("列");
                sb.append(": ");
            }
            sb.append(message);
            return sb.toString();
        }
    }

    public TemplateValidationException(TemplateValidationService.ValidationResult result) {
        super("Template validation failed: " + result.getErrors().size() + " error(s) found");
        this.valid = result.isValid();
        this.errors = new ArrayList<>();
        this.institutionCount = result.getInstitutionCount();
        this.indicatorCount = result.getIndicatorCount();

        for (var err : result.getErrors()) {
            errors.add(new ValidationErrorInfo(err.getCode(), err.getMessage(), err.getSheet(), err.getRow(), err.getColumn()));
        }

        this.warnings = new ArrayList<>();
        for (var warn : result.getWarnings()) {
            warnings.add(new ValidationWarningInfo(warn.getCode(), warn.getMessage(), warn.getSheet(), warn.getRow(), warn.getColumn()));
        }
    }

    public boolean isValid() { return valid; }
    public List<ValidationErrorInfo> getErrors() { return errors; }
    public List<ValidationWarningInfo> getWarnings() { return warnings; }
    public int getInstitutionCount() { return institutionCount; }
    public int getIndicatorCount() { return indicatorCount; }

    /**
     * 获取用户友好的错误摘要
     */
    public String getUserMessage() {
        StringBuilder sb = new StringBuilder();

        if (!errors.isEmpty()) {
            sb.append("❌ 模板存在以下错误，请修正后重新上传：\n\n");
            for (ValidationErrorInfo error : errors) {
                sb.append("• ").append(error.toUserMessage()).append("\n");
            }
        }

        if (!warnings.isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("⚠️  警告（不影响创建，但建议修正）：\n\n");
            for (ValidationWarningInfo warning : warnings) {
                sb.append("• ").append(warning.toUserMessage()).append("\n");
            }
        }

        return sb.length() > 0 ? sb.toString() : "模板校验通过";
    }

    /**
     * 获取详细的校验报告
     */
    public Map<String, Object> getDetailedReport() {
        Map<String, Object> report = new java.util.LinkedHashMap<>();
        report.put("valid", valid);

        List<String> errorMessages = new ArrayList<>();
        for (ValidationErrorInfo err : errors) {
            errorMessages.add(err.toUserMessage());
        }
        report.put("errors", errorMessages);

        List<String> warningMessages = new ArrayList<>();
        for (ValidationWarningInfo warn : warnings) {
            warningMessages.add(warn.toUserMessage());
        }
        report.put("warnings", warningMessages);

        report.put("institutionCount", institutionCount);
        report.put("indicatorCount", indicatorCount);
        report.put("errorCount", errors.size());
        report.put("warningCount", warnings.size());
        report.put("summary", getUserMessage());

        return report;
    }
}
