package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.entity.Skill;
import com.cmbchina.orgperformance.service.SkillService;
import com.cmbchina.orgperformance.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    @Autowired
    private SkillService skillService;

    @GetMapping
    public ApiResponse<List<SkillVO>> getAllSkills(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String category) {
        List<Skill> skills;
        if (category != null && !category.isEmpty()) {
            skills = skillService.getSkillsByCategory(category);
        } else {
            skills = skillService.getAllSkills();
        }
        
        Long userId = getUserId(user);
        List<SkillVO> skillVOList = skills.stream().map(skill -> toSkillVO(skill, userId)).collect(Collectors.toList());
        
        return ApiResponse.success(skillVOList);
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(skillService.getCategories());
    }

    @GetMapping("/tools")
    public ApiResponse<List<SkillService.ToolInfo>> getAvailableTools() {
        return ApiResponse.success(skillService.getAvailableTools());
    }

    @GetMapping("/installed")
    public ApiResponse<List<SkillVO>> getInstalledSkills(@AuthenticationPrincipal UserDetails user) {
        Long userId = getUserId(user);
        List<Skill> skills = skillService.getInstalledSkills(userId);
        List<SkillVO> skillVOList = skills.stream().map(skill -> toSkillVO(skill, userId)).collect(Collectors.toList());
        return ApiResponse.success(skillVOList);
    }

    @PostMapping("/{id}/install")
    public ApiResponse<Void> installSkill(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        Long userId = getUserId(user);
        skillService.installSkill(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/uninstall")
    public ApiResponse<Void> uninstallSkill(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        Long userId = getUserId(user);
        skillService.uninstallSkill(userId, id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillVO> getSkillById(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        Skill skill = skillService.getSkillById(id);
        if (skill == null) {
            return ApiResponse.error(404, "Skill not found");
        }
        Long userId = getUserId(user);
        return ApiResponse.success(toSkillVO(skill, userId));
    }

    @PostMapping
    public ApiResponse<Long> createSkill(@RequestBody SkillRequest request) {
        Skill skill = new Skill();
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setIcon(request.getIcon() != null ? request.getIcon() : "ToolOutlined");
        skill.setCategory(request.getCategory() != null ? request.getCategory() : "自定义");
        skill.setPrompt(request.getPrompt());
        skill.setTools(request.getTools());
        skill.setMarkdownContent(request.getMarkdownContent());
        skill.setScriptContent(request.getScriptContent());
        skill.setVersion(request.getVersion() != null ? request.getVersion() : "1.0.0");
        skill.setAuthor(request.getAuthor());
        skill.setIsBuiltIn(0);
        skill.setIsActive(1);
        
        Long id = skillService.createSkill(skill);
        return ApiResponse.success("技能创建成功", id);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateSkill(@PathVariable Long id, @RequestBody SkillRequest request) {
        Skill skill = skillService.getSkillById(id);
        if (skill == null) {
            return ApiResponse.error(404, "Skill not found");
        }
        if (skill.getIsBuiltIn() != null && skill.getIsBuiltIn() == 1) {
            return ApiResponse.error(403, "内置技能不可修改");
        }
        
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        if (request.getIcon() != null) skill.setIcon(request.getIcon());
        if (request.getCategory() != null) skill.setCategory(request.getCategory());
        if (request.getPrompt() != null) skill.setPrompt(request.getPrompt());
        if (request.getTools() != null) skill.setTools(request.getTools());
        if (request.getMarkdownContent() != null) skill.setMarkdownContent(request.getMarkdownContent());
        if (request.getScriptContent() != null) skill.setScriptContent(request.getScriptContent());
        if (request.getVersion() != null) skill.setVersion(request.getVersion());
        if (request.getAuthor() != null) skill.setAuthor(request.getAuthor());
        
        skillService.updateSkill(skill);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteSkill(@PathVariable Long id) {
        Skill skill = skillService.getSkillById(id);
        if (skill == null) {
            return ApiResponse.error(404, "Skill not found");
        }
        if (skill.getIsBuiltIn() != null && skill.getIsBuiltIn() == 1) {
            return ApiResponse.error(403, "内置技能不可删除");
        }
        
        skillService.deleteSkill(id);
        return ApiResponse.success();
    }

    private SkillVO toSkillVO(Skill skill, Long userId) {
        SkillVO vo = new SkillVO();
        vo.setId(skill.getId());
        vo.setName(skill.getName());
        vo.setDescription(skill.getDescription());
        vo.setIcon(skill.getIcon());
        vo.setCategory(skill.getCategory());
        vo.setPrompt(skill.getPrompt());
        vo.setTools(skill.getTools());
        vo.setInstalled(skillService.isSkillInstalled(userId, skill.getId()));
        vo.setMarkdownContent(skill.getMarkdownContent());
        vo.setScriptPath(skill.getScriptPath());
        vo.setScriptContent(skill.getScriptContent());
        vo.setVersion(skill.getVersion());
        vo.setAuthor(skill.getAuthor());
        return vo;
    }

    private Long getUserId(UserDetails user) {
        if (user == null) {
            return 0L;
        }
        try {
            return Long.parseLong(user.getUsername().split(",")[0]);
        } catch (Exception e) {
            return 0L;
        }
    }

    public static class SkillVO {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private String category;
        private String prompt;
        private String tools;
        private boolean installed;
        private String markdownContent;
        private String scriptPath;
        private String scriptContent;
        private String version;
        private String author;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getTools() { return tools; }
        public void setTools(String tools) { this.tools = tools; }
        public boolean isInstalled() { return installed; }
        public void setInstalled(boolean installed) { this.installed = installed; }
        public String getMarkdownContent() { return markdownContent; }
        public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
        public String getScriptPath() { return scriptPath; }
        public void setScriptPath(String scriptPath) { this.scriptPath = scriptPath; }
        public String getScriptContent() { return scriptContent; }
        public void setScriptContent(String scriptContent) { this.scriptContent = scriptContent; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }

    public static class SkillRequest {
        private String name;
        private String description;
        private String icon;
        private String category;
        private String prompt;
        private String tools;
        private String markdownContent;
        private String scriptContent;
        private String version;
        private String author;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getTools() { return tools; }
        public void setTools(String tools) { this.tools = tools; }
        public String getMarkdownContent() { return markdownContent; }
        public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
        public String getScriptContent() { return scriptContent; }
        public void setScriptContent(String scriptContent) { this.scriptContent = scriptContent; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }
}
