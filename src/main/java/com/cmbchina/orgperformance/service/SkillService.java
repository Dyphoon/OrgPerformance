package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.entity.Skill;
import com.cmbchina.orgperformance.entity.SysUser;
import com.cmbchina.orgperformance.entity.UserSkill;
import com.cmbchina.orgperformance.mapper.SkillMapper;
import com.cmbchina.orgperformance.mapper.SysUserMapper;
import com.cmbchina.orgperformance.mapper.UserSkillMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SkillService {

    private static final Logger logger = LoggerFactory.getLogger(SkillService.class);

    private static final List<ToolInfo> AVAILABLE_TOOLS = Arrays.asList(
        new ToolInfo("list_systems", "查询评估系统列表，支持按名称、状态筛选和分页", "系统管理"),
        new ToolInfo("get_system", "获取指定评估系统的详细信息", "系统管理"),
        new ToolInfo("create_system", "创建新的评估系统", "系统管理"),
        new ToolInfo("upload_system_template", "上传并验证评估系统的Excel模板文件", "系统管理"),
        new ToolInfo("parse_system_template", "解析并验证已上传的Excel模板文件", "系统管理"),
        new ToolInfo("get_system_institutions", "获取指定系统下的所有机构", "系统管理"),
        new ToolInfo("get_system_indicators", "获取指定系统下的所有指标", "系统管理"),
        new ToolInfo("get_system_groups", "获取指定系统下的所有分组名称", "系统管理"),
        new ToolInfo("validate_template", "验证模板数据是否符合系统格式要求", "系统管理"),
        new ToolInfo("list_monitorings", "查询监测任务列表，支持按系统、状态、年份、月份筛选和分页", "监测管理"),
        new ToolInfo("get_monitoring", "获取指定监测任务的详细信息", "监测管理"),
        new ToolInfo("create_monitoring", "创建新的监测任务用于数据采集", "监测管理"),
        new ToolInfo("start_monitoring", "启动监测任务的数据采集阶段", "监测管理"),
        new ToolInfo("close_monitoring", "关闭监测任务，停止数据采集", "监测管理"),
        new ToolInfo("start_confirming", "启动监测任务的确认阶段", "监测管理"),
        new ToolInfo("publish_monitoring", "发布监测任务（使结果可见）", "监测管理"),
        new ToolInfo("rollback_monitoring", "回滚监测任务到数据采集阶段", "监测管理"),
        new ToolInfo("list_tasks", "查询监测任务下的数据采集任务", "任务管理"),
        new ToolInfo("get_my_tasks", "获取指定采集员的任务列表", "任务管理"),
        new ToolInfo("submit_task", "提交数据采集任务的实际值", "任务管理"),
        new ToolInfo("batch_submit_tasks", "批量提交多个采集任务", "任务管理"),
        new ToolInfo("confirm_institution", "确认指定机构的数据", "任务管理"),
        new ToolInfo("get_confirmation_tasks", "获取监测任务的确认任务列表", "任务管理"),
        new ToolInfo("get_overview", "获取监测任务的概览统计信息", "报表管理"),
        new ToolInfo("get_institution_report", "获取指定机构在监测任务中的详细报告", "报表管理"),
        new ToolInfo("generate_reports", "为监测任务下的所有机构生成Excel报告", "报表管理"),
        new ToolInfo("get_performance_data", "获取可自定义指标和维度的绩效数据", "数据分析"),
        new ToolInfo("get_visualization_data", "获取用于生成可视化图表的数据", "数据分析"),
        new ToolInfo("mcporter_list", "列出所有已配置的 MCP 服务器，支持按名称筛选和查看 schema", "MCP管理"),
        new ToolInfo("mcporter_call", "调用指定 MCP 服务器的工具，支持冒号分隔和函数调用两种格式", "MCP管理"),
        new ToolInfo("mcporter_generate_cli", "将 MCP 服务器定义为独立的 CLI 工具", "MCP管理"),
        new ToolInfo("mcporter_emit_ts", "生成 TypeScript 类型定义文件或客户端包装器", "MCP管理"),
        new ToolInfo("execute_command", "执行 shell 命令，返回命令输出结果", "系统工具")
    );

    @Autowired
    private SkillMapper skillMapper;

    @Autowired
    private UserSkillMapper userSkillMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private final Map<String, String> skillMarkdownFiles = new HashMap<>();
    private final Map<String, String> skillScriptFiles = new HashMap<>();

    @PostConstruct
    public void init() {
        initializeSkillFiles();
        initializeBuiltInSkills();
    }

    private void initializeSkillFiles() {
        skillMarkdownFiles.put("数据分析专家", "skills/data_analysis.md");
        skillMarkdownFiles.put("报告撰写助手", "skills/report_writer.md");
        skillMarkdownFiles.put("指标管理专家", "skills/indicator_expert.md");
        skillMarkdownFiles.put("任务提醒助手", "skills/reminder_assistant.md");
        skillMarkdownFiles.put("可视化设计助手", "skills/visual_expert.md");
        skillMarkdownFiles.put("MCPorter助手", "skills/mcporter.md");
    }

    private String loadMarkdownContent(String skillName) {
        String filePath = skillMarkdownFiles.get(skillName);
        if (filePath == null) {
            return null;
        }

        try {
            Path path = Paths.get(System.getProperty("user.dir"), filePath);
            if (Files.exists(path)) {
                return Files.readString(path);
            }

            Resource resource = new ClassPathResource(filePath);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes());
            }
        } catch (IOException e) {
            logger.warn("Failed to load markdown file for skill {}: {}", skillName, e.getMessage());
        }
        return null;
    }

    private void initializeBuiltInSkills() {
        List<Skill> builtInSkills = skillMapper.selectBuiltIn();
        if (builtInSkills.isEmpty()) {
            logger.info("Initializing built-in skills...");

            insertDataAnalysisSkill();
            insertReportWriterSkill();
            insertIndicatorExpertSkill();
            insertReminderExpertSkill();
            insertVisualExpertSkill();
            insertMcporterSkill();

            logger.info("Built-in skills initialized successfully");
        } else {
            // 检查并添加缺失的内置技能
            checkAndInsertMissingSkills(builtInSkills);
            updateSkillMarkdownContent();
            // 自动安装所有内置技能给所有活跃用户
            autoInstallSkillsForAllUsers();
        }
    }

    private void checkAndInsertMissingSkills(List<Skill> existingSkills) {
        // 检查 MCPorter 助手
        boolean mcporterExists = existingSkills.stream()
                .anyMatch(s -> "MCPorter助手".equals(s.getName()));
        if (!mcporterExists) {
            logger.info("Inserting missing MCPorter skill...");
            insertMcporterSkill();
        }

        // 可以在这里添加更多内置技能的检查
    }

    /**
     * 自动将所有内置技能安装给所有活跃用户
     */
    public void autoInstallSkillsForAllUsers() {
        try {
            List<Skill> builtInSkills = skillMapper.selectBuiltIn();
            if (builtInSkills.isEmpty()) {
                logger.info("No built-in skills to auto-install");
                return;
            }

            List<SysUser> activeUsers = sysUserMapper.selectAllActive();
            if (activeUsers.isEmpty()) {
                logger.info("No active users to install skills for");
                return;
            }

            int installedCount = 0;
            for (SysUser user : activeUsers) {
                for (Skill skill : builtInSkills) {
                    if (installSkillForUser(user.getId(), skill.getId())) {
                        installedCount++;
                    }
                }
            }

            logger.info("Auto-installed {} built-in skills for {} users", installedCount, activeUsers.size());
        } catch (Exception e) {
            logger.warn("Failed to auto-install skills: {}", e.getMessage());
        }
    }

    /**
     * 为指定用户安装指定技能（内部方法）
     */
    private boolean installSkillForUser(Long userId, Long skillId) {
        UserSkill userSkill = userSkillMapper.selectByUserIdAndSkillId(userId, skillId);
        if (userSkill == null) {
            userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillId(skillId);
            userSkill.setIsInstalled(1);
            userSkill.setInstalledAt(LocalDateTime.now());
            return userSkillMapper.insert(userSkill) > 0;
        } else if (userSkill.getIsInstalled() == 0) {
            userSkill.setIsInstalled(1);
            userSkill.setInstalledAt(LocalDateTime.now());
            return userSkillMapper.update(userSkill) > 0;
        }
        return false;
    }

    private void updateSkillMarkdownContent() {
        List<Skill> allSkills = skillMapper.selectAll();
        for (Skill skill : allSkills) {
            String markdownContent = loadMarkdownContent(skill.getName());
            if (markdownContent != null && (skill.getMarkdownContent() == null || skill.getMarkdownContent().isEmpty())) {
                skill.setMarkdownContent(markdownContent);
                skillMapper.update(skill);
                logger.info("Updated markdown content for skill: {}", skill.getName());
            }
        }
    }

    private void insertDataAnalysisSkill() {
        Skill dataAnalysis = new Skill();
        dataAnalysis.setName("数据分析专家");
        dataAnalysis.setDescription("专业的绩效数据分析助手，擅长数据分析、趋势洞察、图表解读");
        dataAnalysis.setIcon("BarChartOutlined");
        dataAnalysis.setCategory("数据分析");
        dataAnalysis.setPrompt("你是一个专业的数据分析专家。当用户询问数据分析相关问题时，请：1. 提供清晰的数据解读 2. 分析趋势和模式 3. 用直观的方式展示结论");
        dataAnalysis.setTools("getMonitoringTasks,getReportData");
        dataAnalysis.setMarkdownContent(loadMarkdownContent("数据分析专家"));
        dataAnalysis.setVersion("1.0.0");
        dataAnalysis.setAuthor("System");
        dataAnalysis.setIsBuiltIn(1);
        dataAnalysis.setIsActive(1);
        skillMapper.insert(dataAnalysis);
    }

    private void insertReportWriterSkill() {
        Skill reportWriter = new Skill();
        reportWriter.setName("报告撰写助手");
        reportWriter.setDescription("专业的绩效报告撰写助手，擅长生成结构清晰、内容丰富的报告");
        reportWriter.setIcon("FileTextOutlined");
        reportWriter.setCategory("文档处理");
        reportWriter.setPrompt("你是一个专业的报告撰写助手。擅长撰写各类绩效报告，包括：月度监测报告、季度分析报告、年度总结报告等。请确保报告结构清晰、数据准确、分析到位。");
        reportWriter.setTools("getReportData,generateReport");
        reportWriter.setMarkdownContent(loadMarkdownContent("报告撰写助手"));
        reportWriter.setVersion("1.0.0");
        reportWriter.setAuthor("System");
        reportWriter.setIsBuiltIn(1);
        reportWriter.setIsActive(1);
        skillMapper.insert(reportWriter);
    }

    private void insertIndicatorExpertSkill() {
        Skill indicatorExpert = new Skill();
        indicatorExpert.setName("指标管理专家");
        indicatorExpert.setDescription("专业的绩效指标管理助手，擅长指标设计、体系构建、权重分配");
        indicatorExpert.setIcon("DashboardOutlined");
        indicatorExpert.setCategory("指标管理");
        indicatorExpert.setPrompt("你是一个专业的绩效指标管理专家。擅长：1. 设计科学的绩效指标体系 2. 合理分配指标权重 3. 提供指标定义和计算方法指导");
        indicatorExpert.setTools("getAssessmentSystems,getIndicatorsBySystem");
        indicatorExpert.setMarkdownContent(loadMarkdownContent("指标管理专家"));
        indicatorExpert.setVersion("1.0.0");
        indicatorExpert.setAuthor("System");
        indicatorExpert.setIsBuiltIn(1);
        indicatorExpert.setIsActive(1);
        skillMapper.insert(indicatorExpert);
    }

    private void insertReminderExpertSkill() {
        Skill reminderExpert = new Skill();
        reminderExpert.setName("任务提醒助手");
        reminderExpert.setDescription("智能的任务监测和提醒助手，帮助追踪绩效任务进度");
        reminderExpert.setIcon("BellOutlined");
        reminderExpert.setCategory("任务管理");
        reminderExpert.setPrompt("你是一个任务提醒助手。帮助用户追踪绩效数据的收集和确认任务，确保各项任务按时完成。及时提醒即将到期和已超期的任务。");
        reminderExpert.setTools("getMonitoringTasks,getCollectionTasks,getConfirmationTasks");
        reminderExpert.setMarkdownContent(loadMarkdownContent("任务提醒助手"));
        reminderExpert.setVersion("1.0.0");
        reminderExpert.setAuthor("System");
        reminderExpert.setIsBuiltIn(1);
        reminderExpert.setIsActive(1);
        skillMapper.insert(reminderExpert);
    }

    private void insertVisualExpertSkill() {
        Skill visualExpert = new Skill();
        visualExpert.setName("可视化设计助手");
        visualExpert.setDescription("专业的可视化图表设计助手，帮助创建直观的绩效展示");
        visualExpert.setIcon("PieChartOutlined");
        visualExpert.setCategory("数据可视化");
        visualExpert.setPrompt("你是一个数据可视化专家。帮助用户创建直观的图表展示，包括：趋势图、对比图、分布图、仪表盘等。让复杂的数据变得易于理解。");
        visualExpert.setTools("getReportData,getMonitoringTasks");
        visualExpert.setMarkdownContent(loadMarkdownContent("可视化设计助手"));
        visualExpert.setVersion("1.0.0");
        visualExpert.setAuthor("System");
        visualExpert.setIsBuiltIn(1);
        visualExpert.setIsActive(1);
        skillMapper.insert(visualExpert);
    }

    private void insertMcporterSkill() {
        Skill mcporter = new Skill();
        mcporter.setName("MCPorter助手");
        mcporter.setDescription("MCPorter MCP 工具调用助手，帮助发现、调用和管理 MCP 服务器");
        mcporter.setIcon("ApiOutlined");
        mcporter.setCategory("MCP管理");
        mcporter.setPrompt("你是一个 MCPorter 助手。MCPorter 是一个 TypeScript 运行时和 CLI 工具，用于调用 Model Context Protocol (MCP) 服务器。擅长：1. 发现和列出已配置的 MCP 服务器 2. 调用 MCP 工具执行操作 3. 生成类型化客户端代码 4. 创建独立的 CLI 工具。常用命令：npx mcporter list（列出服务器）、npx mcporter call server.tool（调用工具）、npx mcporter emit-ts（生成类型）。");
        mcporter.setTools("mcporter_list,mcporter_call,mcporter_generate_cli,mcporter_emit_ts");
        mcporter.setMarkdownContent(loadMarkdownContent("MCPorter助手"));
        mcporter.setVersion("1.0.0");
        mcporter.setAuthor("System");
        mcporter.setIsBuiltIn(1);
        mcporter.setIsActive(1);
        skillMapper.insert(mcporter);
    }

    public List<Skill> getAllSkills() {
        return skillMapper.selectAll();
    }

    public List<Skill> getSkillsByCategory(String category) {
        return skillMapper.selectByCategory(category);
    }

    public List<String> getCategories() {
        List<Skill> allSkills = skillMapper.selectAll();
        return allSkills.stream()
                .map(Skill::getCategory)
                .distinct()
                .sorted()
                .toList();
    }

    public Skill getSkillById(Long id) {
        return skillMapper.selectById(id);
    }

    public List<Skill> getInstalledSkills(Long userId) {
        return skillMapper.selectInstalledByUserId(userId);
    }

    public boolean installSkill(Long userId, Long skillId) {
        UserSkill userSkill = userSkillMapper.selectByUserIdAndSkillId(userId, skillId);
        if (userSkill == null) {
            userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillId(skillId);
            userSkill.setIsInstalled(1);
            userSkill.setInstalledAt(LocalDateTime.now());
            return userSkillMapper.insert(userSkill) > 0;
        } else {
            userSkill.setIsInstalled(1);
            userSkill.setInstalledAt(LocalDateTime.now());
            return userSkillMapper.update(userSkill) > 0;
        }
    }

    public boolean uninstallSkill(Long userId, Long skillId) {
        UserSkill userSkill = userSkillMapper.selectByUserIdAndSkillId(userId, skillId);
        if (userSkill != null) {
            userSkill.setIsInstalled(0);
            return userSkillMapper.update(userSkill) > 0;
        }
        return false;
    }

    public boolean isSkillInstalled(Long userId, Long skillId) {
        UserSkill userSkill = userSkillMapper.selectByUserIdAndSkillId(userId, skillId);
        return userSkill != null && userSkill.getIsInstalled() == 1;
    }

    public Long createSkill(Skill skill) {
        skillMapper.insert(skill);
        logger.info("Created new skill: {}", skill.getName());
        return skill.getId();
    }

    public void updateSkill(Skill skill) {
        skillMapper.update(skill);
        logger.info("Updated skill: {}", skill.getName());
    }

    public void deleteSkill(Long id) {
        skillMapper.deleteById(id);
        logger.info("Deleted skill with id: {}", id);
    }

    public List<ToolInfo> getAvailableTools() {
        return AVAILABLE_TOOLS;
    }

    public static class ToolInfo {
        private String name;
        private String description;
        private String category;

        public ToolInfo(String name, String description, String category) {
            this.name = name;
            this.description = description;
            this.category = category;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
    }
}
