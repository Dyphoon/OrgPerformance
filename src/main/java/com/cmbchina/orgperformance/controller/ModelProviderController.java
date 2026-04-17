package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.entity.ModelProvider;
import com.cmbchina.orgperformance.service.ModelProviderService;
import com.cmbchina.orgperformance.vo.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model-providers")
public class ModelProviderController {

    @Autowired
    private ModelProviderService modelProviderService;

    @GetMapping
    public ApiResponse<List<ModelProvider>> list() {
        return ApiResponse.success(modelProviderService.getAll());
    }

    @GetMapping("/active")
    public ApiResponse<List<ModelProvider>> listActive() {
        return ApiResponse.success(modelProviderService.getAllActive());
    }

    @GetMapping("/{id}")
    public ApiResponse<ModelProvider> getById(@PathVariable Long id) {
        return ApiResponse.success(modelProviderService.getById(id));
    }

    @PostMapping
    public ApiResponse<ModelProvider> create(@RequestBody ModelProvider modelProvider) {
        return ApiResponse.success(modelProviderService.create(modelProvider));
    }

    @PutMapping("/{id}")
    public ApiResponse<ModelProvider> update(@PathVariable Long id, @RequestBody ModelProvider modelProvider) {
        modelProvider.setId(id);
        return ApiResponse.success(modelProviderService.update(modelProvider));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        modelProviderService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/init")
    public ApiResponse<String> initDefault() {
        modelProviderService.initDefaultProviders();
        return ApiResponse.success("Default providers initialized");
    }
}