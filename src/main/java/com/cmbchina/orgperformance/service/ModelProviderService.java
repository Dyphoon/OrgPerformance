package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.entity.ModelProvider;
import com.cmbchina.orgperformance.mapper.ModelProviderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ModelProviderService {

    @Autowired
    private ModelProviderMapper modelProviderMapper;

    public List<ModelProvider> getAll() {
        return modelProviderMapper.findAll();
    }

    public List<ModelProvider> getAllActive() {
        return modelProviderMapper.findAllActive();
    }

    public ModelProvider getById(Long id) {
        return modelProviderMapper.findById(id);
    }

    public ModelProvider getByCode(String code) {
        return modelProviderMapper.findByCode(code);
    }

    @Transactional
    public ModelProvider create(ModelProvider modelProvider) {
        if (modelProvider.getSortOrder() == null) {
            modelProvider.setSortOrder(0);
        }
        if (modelProvider.getStatus() == null) {
            modelProvider.setStatus(1);
        }
        if (modelProvider.getMaxTokens() == null) {
            modelProvider.setMaxTokens(4096);
        }
        if (modelProvider.getTemperature() == null) {
            modelProvider.setTemperature(0.7);
        }
        if (modelProvider.getApiKey() == null || modelProvider.getApiKey().isEmpty()) {
            modelProvider.setApiKey("PLEASE_CONFIGURE");
        }
        modelProviderMapper.insert(modelProvider);
        return modelProvider;
    }

    @Transactional
    public ModelProvider update(ModelProvider modelProvider) {
        modelProviderMapper.update(modelProvider);
        return modelProvider;
    }

    @Transactional
    public void delete(Long id) {
        modelProviderMapper.deleteById(id);
    }

    public int count() {
        return modelProviderMapper.count();
    }

    @Transactional
    public void initDefaultProviders() {
        if (modelProviderMapper.count() > 0) {
            return;
        }

        String placeholderKey = "PLEASE_CONFIGURE";

        ModelProvider minimax = new ModelProvider();
        minimax.setName("MiniMax");
        minimax.setCode("minimax");
        minimax.setBaseUrl("https://api.minimaxi.com/v1");
        minimax.setApiKey(placeholderKey);
        minimax.setModelName("MiniMax-M2.7");
        minimax.setModelType("minimax");
        minimax.setMaxTokens(196608);
        minimax.setTemperature(0.7);
        minimax.setSortOrder(1);
        minimax.setStatus(1);
        create(minimax);

        ModelProvider glm = new ModelProvider();
        glm.setName("智谱 GLM");
        glm.setCode("glm");
        glm.setBaseUrl("https://open.bigmodel.cn/api/paas/v4");
        glm.setApiKey(placeholderKey);
        glm.setModelName("glm-4");
        glm.setModelType("glm");
        glm.setMaxTokens(128000);
        glm.setTemperature(0.7);
        glm.setSortOrder(2);
        glm.setStatus(1);
        create(glm);
    }
}