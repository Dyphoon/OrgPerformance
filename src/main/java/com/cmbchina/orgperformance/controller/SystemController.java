package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.dto.SystemCreateRequest;
import com.cmbchina.orgperformance.entity.Institution;
import com.cmbchina.orgperformance.entity.Indicator;
import com.cmbchina.orgperformance.service.AssessmentSystemService;
import com.cmbchina.orgperformance.vo.ApiResponse;
import com.cmbchina.orgperformance.vo.PageVO;
import com.cmbchina.orgperformance.vo.SystemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/systems")
public class SystemController {

    @Autowired
    private AssessmentSystemService systemService;

    @GetMapping
    public ApiResponse<PageVO<List<SystemVO>>> getSystemList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<SystemVO> list = systemService.getSystemList(name, status, page, pageSize);
        int total = systemService.count(name, status);
        return ApiResponse.success(new PageVO<>((long) total, page, pageSize, list));
    }

    @GetMapping("/{id}")
    public ApiResponse<SystemVO> getSystemById(@PathVariable Long id) {
        SystemVO system = systemService.getSystemById(id);
        if (system == null) {
            return ApiResponse.error(404, "System not found");
        }
        return ApiResponse.success(system);
    }

    @GetMapping("/{id}/institutions")
    public ApiResponse<List<Institution>> getInstitutions(@PathVariable Long id) {
        List<Institution> institutions = systemService.getInstitutionsBySystemId(id);
        return ApiResponse.success(institutions);
    }

    @GetMapping("/{id}/indicators")
    public ApiResponse<List<Indicator>> getIndicators(@PathVariable Long id) {
        List<Indicator> indicators = systemService.getIndicatorsBySystemId(id);
        return ApiResponse.success(indicators);
    }

    @GetMapping("/{id}/groups")
    public ApiResponse<List<String>> getGroupNames(@PathVariable Long id) {
        List<String> groups = systemService.getGroupNamesBySystemId(id);
        return ApiResponse.success(groups);
    }

    @GetMapping("/{id}/template-url")
    public ApiResponse<String> getTemplateUrl(@PathVariable Long id) {
        String url = systemService.getTemplateDownloadUrl(id);
        return ApiResponse.success(url);
    }

    @PostMapping
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Long> createSystem(@RequestBody SystemCreateRequest request) {
        Long systemId = systemService.createSystemWithParsedData(request);
        return ApiResponse.success("System created", systemId);
    }

    @PostMapping("/with-file")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Long> createSystemWithFile(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "needApproval", defaultValue = "false") Boolean needApproval,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long systemId = systemService.createSystemWithExcel(name, description, needApproval, file);
        return ApiResponse.success("System created", systemId);
    }

    @PostMapping("/with-data")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Long> createSystemWithData(@RequestBody SystemCreateRequest request) {
        Long systemId = systemService.createSystemWithParsedData(request);
        return ApiResponse.success("System created", systemId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> updateSystem(@PathVariable Long id, @RequestBody SystemCreateRequest request) {
        systemService.updateSystem(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('1')")
    public ApiResponse<Void> deleteSystem(@PathVariable Long id) {
        systemService.deleteSystem(id);
        return ApiResponse.success();
    }
}
