package com.cmbchina.orgperformance.excel;

import com.cmbchina.orgperformance.entity.Indicator;
import com.cmbchina.orgperformance.entity.Institution;
import com.cmbchina.orgperformance.entity.CollectionTask;
import com.cmbchina.orgperformance.mapper.IndicatorMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class ExcelGenerator {

    private static final String SHEET_TEMPLATE = "模版页";
    private static final String SHEET_DATA_COLLECTION = "数据收集页";
    private static final String SHEET_PARAMS = "参数页";

    @Autowired
    private IndicatorMapper indicatorMapper;

    public byte[] createCollectorExcel(byte[] templateData, List<DataCollectionSheetParser.CollectionIndicator> collectorIndicators,
                                       List<Institution> institutions, String collectorName,
                                       Integer year, Integer month) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(templateData))) {
            modifyDataCollectionSheetFromCollectionIndicators(workbook, collectorIndicators, institutions, collectorName);

            // 删除除"数据收集页"之外的所有其他sheet页
            List<String> sheetsToRemove = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String sheetName = workbook.getSheetAt(i).getSheetName();
                if (!SHEET_DATA_COLLECTION.equals(sheetName)) {
                    sheetsToRemove.add(sheetName);
                }
            }
            // 从后往前删除，避免索引变化
            for (int i = sheetsToRemove.size() - 1; i >= 0; i--) {
                int idx = workbook.getSheetIndex(sheetsToRemove.get(i));
                if (idx >= 0) {
                    workbook.removeSheetAt(idx);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void modifyDataCollectionSheetFromCollectionIndicators(Workbook workbook, List<DataCollectionSheetParser.CollectionIndicator> collectorIndicators,
                                          List<Institution> institutions, String collectorName) {
        Sheet originalSheet = workbook.getSheet(SHEET_DATA_COLLECTION);
        if (originalSheet == null) return;

        // 删除原"数据收集页"，用"数据收集页_收数"替代
        int sheetIndex = workbook.getSheetIndex(originalSheet);
        workbook.removeSheetAt(sheetIndex);

        Sheet newSheet = workbook.createSheet("数据收集页");

        // 复制表头行
        Row origHeader1 = originalSheet.getRow(1);
        Row origHeader2 = originalSheet.getRow(2);
        Row origHeader3 = originalSheet.getRow(3);
        Row origHeader4 = originalSheet.getRow(4);

        if (origHeader1 == null) return;

        Row newHeader1 = newSheet.createRow(0);
        Row newHeader2 = newSheet.createRow(1);
        Row newHeader3 = newSheet.createRow(2);
        Row newHeader4 = newSheet.createRow(3);

        CellStyle headerStyle = getOrCreateHeaderStyle(workbook);

        // 第一列：机构名称
        Cell cell1 = newHeader1.createCell(0);
        cell1.setCellValue(origHeader1.getCell(0) != null ? origHeader1.getCell(0).getStringCellValue() : "机构名称");
        cell1.setCellStyle(headerStyle);

        Cell cell2 = newHeader2.createCell(0);
        cell2.setCellValue(origHeader2 != null && origHeader2.getCell(0) != null ?
                origHeader2.getCell(0).getStringCellValue() : "");
        cell2.setCellStyle(headerStyle);

        Cell cell3 = newHeader3.createCell(0);
        cell3.setCellValue(origHeader3 != null && origHeader3.getCell(0) != null ?
                origHeader3.getCell(0).getStringCellValue() : "");
        cell3.setCellStyle(headerStyle);

        Cell cell4 = newHeader4.createCell(0);
        cell4.setCellValue(origHeader4 != null && origHeader4.getCell(0) != null ?
                origHeader4.getCell(0).getStringCellValue() : "");
        cell4.setCellStyle(headerStyle);

        // 根据收数指标创建列
        int colIndex = 1;
        Map<Integer, Integer> origColToNewColMap = new HashMap<>();

        for (DataCollectionSheetParser.CollectionIndicator colInd : collectorIndicators) {
            int origCol = colInd.getColumnIndex();
            origColToNewColMap.put(origCol, colIndex);

            Cell newCell1 = newHeader1.createCell(colIndex);
            newCell1.setCellValue(colInd.getIndicatorName());
            newCell1.setCellStyle(headerStyle);

            Cell newCell2 = newHeader2.createCell(colIndex);
            newCell2.setCellValue(collectorName);
            newCell2.setCellStyle(headerStyle);

            Cell newCell3 = newHeader3.createCell(colIndex);
            newCell3.setCellValue(colInd.getUnit() != null ? colInd.getUnit() : "");
            newCell3.setCellStyle(headerStyle);

            Cell newCell4 = newHeader4.createCell(colIndex);
            newCell4.setCellValue("");
            newCell4.setCellStyle(headerStyle);

            colIndex++;
        }

        // 复制机构数据行（从原始第5行开始，即Excel第6行，因为新sheet的0-3行是表头，数据从第5行开始）
        for (int rowNum = 5; rowNum <= originalSheet.getLastRowNum(); rowNum++) {
            Row origRow = originalSheet.getRow(rowNum);
            if (origRow == null || isEmptyRow(origRow)) continue;

            String instName = getCellStringValue(origRow.getCell(0));
            if (instName == null || instName.isEmpty()) continue;

            Row newRow = newSheet.createRow(rowNum - 1);  // 原始第5行 → 新第4行
            Cell instCell = newRow.createCell(0);
            instCell.setCellValue(instName);

            for (Map.Entry<Integer, Integer> entry : origColToNewColMap.entrySet()) {
                int origCol = entry.getKey();
                int newCol = entry.getValue();
                Cell origDataCell = origRow.getCell(origCol);
                Cell newDataCell = newRow.createCell(newCol);
                if (origDataCell != null) {
                    copyCellValue(newDataCell, origDataCell);
                }
            }
        }
    }

    private void modifyParamsSheet(Workbook workbook, Integer year, Integer month, String collectorName) {
        Sheet paramsSheet = workbook.getSheet(SHEET_PARAMS);
        if (paramsSheet == null) return;

        for (int i = 2; i <= paramsSheet.getLastRowNum(); i++) {
            Row row = paramsSheet.getRow(i);
            if (row == null) continue;
            Cell keyCell = row.getCell(0);
            if (keyCell == null) continue;

            String key = keyCell.getStringCellValue();
            Cell valueCell = row.getCell(1);
            if (valueCell == null) {
                valueCell = row.createCell(1);
            }

            switch (key) {
                case "CURRENT_YEAR":
                    valueCell.setCellValue(year != null ? String.valueOf(year) : "");
                    break;
                case "CURRENT_MONTH":
                    valueCell.setCellValue(month != null ? String.format("%02d", month) : "");
                    break;
                case "CURRENT_COLLECTOR":
                    valueCell.setCellValue(collectorName);
                    break;
                case "COLLECTION_STATUS":
                    valueCell.setCellValue("收数中");
                    break;
            }
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
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

    private void copyCellValue(Cell target, Cell source) {
        if (source == null) return;
        switch (source.getCellType()) {
            case STRING:
                target.setCellValue(source.getStringCellValue());
                break;
            case NUMERIC:
                target.setCellValue(source.getNumericCellValue());
                break;
            case BOOLEAN:
                target.setCellValue(source.getBooleanCellValue());
                break;
            case FORMULA:
                target.setCellFormula(source.getCellFormula());
                break;
            default:
                break;
        }
    }

    private CellStyle getOrCreateHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    public byte[] generateInstitutionReport(byte[] templateData, Institution institution,
                                            Map<String, BigDecimal> indicatorData,
                                            Integer year, Integer month) throws IOException {
        return generateInstitutionReport(templateData, institution, indicatorData, year, month, null);
    }

    public byte[] generateInstitutionReport(byte[] templateData, Institution institution,
                                            Map<String, BigDecimal> indicatorData,
                                            Integer year, Integer month, Long systemId) throws IOException {
        return generateInstitutionReport(templateData, institution, indicatorData, year, month, systemId, null);
    }

    public byte[] generateInstitutionReport(byte[] templateData, Institution institution,
                                            Map<String, BigDecimal> indicatorData,
                                            Integer year, Integer month, Long systemId,
                                            List<CollectionTask> tasksForInstitution) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(templateData))) {
            Sheet sheet = workbook.getSheet(SHEET_DATA_COLLECTION);
            if (sheet != null) {
                fillIndicatorData(sheet, institution.getOrgName(), indicatorData);
            }

            Sheet paramsSheet = workbook.getSheet(SHEET_PARAMS);
            if (paramsSheet != null) {
                updateParamsSheetForInstitution(paramsSheet, institution, year, month);
            }

            // Fill template page with scores if systemId is provided
            if (systemId != null) {
                Sheet templateSheet = workbook.getSheet(SHEET_TEMPLATE);
                if (templateSheet != null) {
                    List<Indicator> indicators = indicatorMapper.selectBySystemId(systemId);
                    fillTemplateScores(templateSheet, indicatorData, indicators, tasksForInstitution);
                }
            }

            // Note: Formula recalculation happens automatically when workbook is opened in Excel

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void fillTemplateScores(Sheet templateSheet,
                                    Map<String, BigDecimal> indicatorData,
                                    List<Indicator> indicators,
                                    List<CollectionTask> tasksForInstitution) {
        if (templateSheet == null || indicatorData == null || indicators == null) {
            return;
        }

        // Build a mapping from indicator name to actual value using tasks if available
        Map<String, BigDecimal> nameToValue = new HashMap<>(indicatorData);
        if (tasksForInstitution != null) {
            for (CollectionTask task : tasksForInstitution) {
                String collectionName = task.getCollectionIndicatorName();
                if (collectionName != null && task.getActualValue() != null) {
                    nameToValue.put(collectionName, task.getActualValue());
                }
            }
        }

        // Collect all indicators by category and dimension for aggregation
        Map<String, BigDecimal> categoryWeights = new HashMap<>();
        Map<String, BigDecimal> dimensionWeights = new HashMap<>();
        Map<String, BigDecimal> categoryScores = new HashMap<>();
        Map<String, BigDecimal> dimensionScores = new HashMap<>();

        // First pass: calculate scores for each indicator
        List<Map<String, Object>> calculatedScores = new ArrayList<>();

        // Template sheet: row 0 = empty, row 1 = headers, row 2 = column headers (一级指标, 二级指标...), row 3+ = data
        // Start from row 4 (index 4) to skip headers and match readTemplateData which reads data from row 4
        for (int rowNum = 4; rowNum <= templateSheet.getLastRowNum(); rowNum++) {
            Row row = templateSheet.getRow(rowNum);
            if (row == null) continue;

            String level1Name = getCellStringValue(row.getCell(2));
            String level2Name = getCellStringValue(row.getCell(3));

            // Find matching indicator
            Indicator indicator = null;
            for (Indicator ind : indicators) {
                boolean match = false;
                if (level2Name != null && !level2Name.isEmpty() &&
                    level2Name.equals(ind.getLevel2Name())) {
                    match = true;
                } else if (level1Name != null && level1Name.equals(ind.getLevel1Name()) &&
                           (ind.getLevel2Name() == null || ind.getLevel2Name().isEmpty())) {
                    match = true;
                }
                if (match) {
                    indicator = ind;
                    break;
                }
            }

            if (indicator == null) {
                continue;
            }

            // Get actual value - try multiple keys
            BigDecimal actualValue = null;

            // First try level2Name
            if (level2Name != null && !level2Name.isEmpty()) {
                actualValue = nameToValue.get(level2Name);
            }

            // Then try level1Name
            if (actualValue == null && level1Name != null) {
                actualValue = nameToValue.get(level1Name);
            }

            // Set actual value (column H, index 8)
            if (actualValue != null) {
                Cell actualCell = row.getCell(8);
                if (actualCell == null) actualCell = row.createCell(8);
                actualCell.setCellValue(actualValue.doubleValue());
            }

            // Calculate and set scores
            BigDecimal progressTarget = indicator.getProgressTarget();
            BigDecimal annualTarget = indicator.getAnnualTarget();
            BigDecimal weight = indicator.getWeight();
            String category = indicator.getCategory();
            String dimension = indicator.getDimension();

            // Use progressTarget if available and non-zero, otherwise fall back to annualTarget
            BigDecimal targetForScore = progressTarget != null && progressTarget.compareTo(BigDecimal.ZERO) != 0
                    ? progressTarget : annualTarget;

            // Progress completion rate = actualValue / progressTarget
            BigDecimal progressCompletionRate = null;
            if (progressTarget != null && progressTarget.compareTo(BigDecimal.ZERO) != 0 && actualValue != null) {
                progressCompletionRate = actualValue.divide(progressTarget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            // Annual completion rate = actualValue / annualTarget
            BigDecimal annualCompletionRate = null;
            if (annualTarget != null && annualTarget.compareTo(BigDecimal.ZERO) != 0 && actualValue != null) {
                annualCompletionRate = actualValue.divide(annualTarget, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            // Score 100 (completion rate capped at 100)
            BigDecimal score100 = null;
            if (targetForScore != null && targetForScore.compareTo(BigDecimal.ZERO) != 0 && actualValue != null) {
                score100 = actualValue.divide(targetForScore, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                score100 = score100.min(BigDecimal.valueOf(100));
            }

            // Score weighted = score100 * weight
            BigDecimal scoreWeighted = null;
            if (score100 != null && weight != null) {
                scoreWeighted = score100.multiply(weight).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            }

            // Set cells (column I-J: indices 9-10 for completion rates, K-L: 11-12 for scores)
            if (progressCompletionRate != null) {
                Cell cell = row.getCell(10);
                if (cell == null) cell = row.createCell(10);
                cell.setCellValue(progressCompletionRate.doubleValue());
            }

            if (annualCompletionRate != null) {
                Cell cell = row.getCell(9);
                if (cell == null) cell = row.createCell(9);
                cell.setCellValue(annualCompletionRate.doubleValue());
            }

            if (score100 != null) {
                Cell cell = row.getCell(11);
                if (cell == null) cell = row.createCell(11);
                cell.setCellValue(score100.doubleValue());
            }

            if (scoreWeighted != null) {
                Cell cell = row.getCell(12);
                if (cell == null) cell = row.createCell(12);
                cell.setCellValue(scoreWeighted.doubleValue());
            }

            // Store for aggregation
            Map<String, Object> calcScore = new HashMap<>();
            calcScore.put("rowNum", rowNum);
            calcScore.put("scoreWeighted", scoreWeighted);
            calcScore.put("weight", weight);
            calcScore.put("category", category);
            calcScore.put("dimension", dimension);
            calcScore.put("level2Name", level2Name);
            calculatedScores.add(calcScore);

            // Accumulate weights
            if (weight != null) {
                categoryWeights.merge(category, weight, BigDecimal::add);
                dimensionWeights.merge(dimension, weight, BigDecimal::add);
            }
        }

        // Second pass: calculate category and dimension scores (weighted average * 100)
        for (Map<String, Object> calcScore : calculatedScores) {
            BigDecimal scoreWeighted = (BigDecimal) calcScore.get("scoreWeighted");
            String category = (String) calcScore.get("category");
            String dimension = (String) calcScore.get("dimension");
            BigDecimal weight = (BigDecimal) calcScore.get("weight");

            if (scoreWeighted != null && weight != null) {
                BigDecimal weightedContribution = scoreWeighted.multiply(weight);
                categoryScores.merge(category, weightedContribution, BigDecimal::add);
                dimensionScores.merge(dimension, weightedContribution, BigDecimal::add);
            }
        }

        // Third pass: set category and dimension scores, and total score
        BigDecimal totalScoreSum = BigDecimal.ZERO;
        for (Map<String, Object> calcScore : calculatedScores) {
            int rowNum = (Integer) calcScore.get("rowNum");
            String category = (String) calcScore.get("category");
            String dimension = (String) calcScore.get("dimension");
            BigDecimal scoreWeighted = (BigDecimal) calcScore.get("scoreWeighted");

            Row row = templateSheet.getRow(rowNum);
            if (row == null) continue;

            // Category score (column M, index 13) = sum of weighted scores for category
            BigDecimal catScore = categoryScores.get(category);
            if (catScore != null) {
                Cell cell = row.getCell(13);
                if (cell == null) cell = row.createCell(13);
                cell.setCellValue(catScore.doubleValue());
            }

            // Dimension score (column N, index 14) = sum of weighted scores for dimension
            BigDecimal dimScore = dimensionScores.get(dimension);
            if (dimScore != null) {
                Cell cell = row.getCell(14);
                if (cell == null) cell = row.createCell(14);
                cell.setCellValue(dimScore.doubleValue());
            }

            // Total score (column O, index 15) = sum of all weighted scores
            if (scoreWeighted != null) {
                totalScoreSum = totalScoreSum.add(scoreWeighted);
                Cell cell = row.getCell(15);
                if (cell == null) cell = row.createCell(15);
                cell.setCellValue(totalScoreSum.doubleValue());
            }
        }
    }

    private void fillIndicatorData(Sheet sheet, String institutionName, Map<String, BigDecimal> indicatorData) {
        if (sheet == null || indicatorData == null) return;

        Row headerRow = sheet.getRow(1);
        if (headerRow == null) return;

        // 数据收集页的机构数据从第4行开始（0-3是表头）
        for (int rowNum = 4; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;

            String instName = getCellStringValue(row.getCell(0));
            if (institutionName != null && institutionName.equals(instName)) {
                for (int col = 1; col < headerRow.getLastCellNum(); col++) {
                    Cell headerCell = headerRow.getCell(col);
                    if (headerCell == null) continue;
                    String indicatorName = headerCell.getStringCellValue();
                    
                    BigDecimal value = indicatorData.get(indicatorName);
                    if (value != null) {
                        Cell dataCell = row.getCell(col);
                        if (dataCell == null) {
                            dataCell = row.createCell(col);
                        }
                        dataCell.setCellValue(value.doubleValue());
                    }
                }
                break;
            }
        }
    }

    private void updateParamsSheetForInstitution(Sheet paramsSheet, Institution institution, Integer year, Integer month) {
        for (int i = 2; i <= paramsSheet.getLastRowNum(); i++) {
            Row row = paramsSheet.getRow(i);
            if (row == null) continue;
            Cell keyCell = row.getCell(0);
            if (keyCell == null) continue;

            String key = getCellStringValue(keyCell);
            if (key == null || key.isEmpty()) continue;

            Cell valueCell = row.getCell(1);
            if (valueCell == null) {
                valueCell = row.createCell(1);
            }

            switch (key) {
                case "CURRENT_DATE":
                    valueCell.setCellType(CellType.BLANK);
                    valueCell.setCellValue(year != null && month != null ? String.format("%d%02d", year, month) : "");
                    break;
                case "CURRENT_YEAR":
                    valueCell.setCellType(CellType.BLANK);
                    valueCell.setCellValue(year != null ? String.valueOf(year) : "");
                    break;
                case "CURRENT_MONTH":
                    valueCell.setCellType(CellType.BLANK);
                    valueCell.setCellValue(month != null ? String.format("%02d", month) : "");
                    break;
                case "CURRENT_PERIOD":
                    valueCell.setCellType(CellType.BLANK);
                    valueCell.setCellValue(year != null && month != null ? String.format("%d%02d", year, month) : "");
                    break;
                case "CURRENT_ORG":
                    valueCell.setCellType(CellType.BLANK);
                    valueCell.setCellValue(institution.getOrgName() != null ? institution.getOrgName() : "");
                    break;
                case "CURRENT_ORG_ID":
                    valueCell.setCellType(CellType.BLANK);
                    valueCell.setCellValue(institution.getOrgId() != null ? institution.getOrgId() : "");
                    break;
                case "COLLECTION_STATUS":
                    valueCell.setCellType(CellType.BLANK);
                    valueCell.setCellValue("已完成");
                    break;
            }
        }
    }
}
