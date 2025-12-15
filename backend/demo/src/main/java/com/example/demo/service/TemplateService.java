package com.example.demo.service;

import com.example.demo.dto.TemplateCreateDTO;
import com.example.demo.entity.Template;
import com.example.demo.repository.TemplateRepository;
import com.example.demo.vo.TemplateVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemplateService {
    @Autowired
    private TemplateRepository templateRepository;
    
    @Autowired
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有模板
     */
    public List<TemplateVO> getAllTemplates() {
        List<Template> templates = templateRepository.findAll();
        return templates.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    /**
     * 获取启用的模板
     */
    public List<TemplateVO> getEnabledTemplates(String category) {
        List<Template> templates;
        if (category != null && !category.isEmpty() && !category.equals("all")) {
            templates = templateRepository.findByStatusAndCategory(1, category);
        } else {
            templates = templateRepository.findByStatus(1);
        }
        return templates.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    /**
     * 根据ID获取模板
     */
    public TemplateVO getTemplateById(Long id) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        return convertToVO(template);
    }

    /**
     * 创建模板
     */
    @Transactional
    public TemplateVO createTemplate(TemplateCreateDTO dto, Long createdBy) {
        Template template = new Template();
        BeanUtils.copyProperties(dto, template);
        template.setCreatedBy(createdBy);
        template.setStatus(1); // 默认启用
        
        Template saved = templateRepository.save(template);
        return convertToVO(saved);
    }

    /**
     * 更新模板
     */
    @Transactional
    public TemplateVO updateTemplate(Long id, TemplateCreateDTO dto) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        
        BeanUtils.copyProperties(dto, template, "id", "createTime", "createdBy");
        
        Template saved = templateRepository.save(template);
        return convertToVO(saved);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new RuntimeException("模板不存在");
        }
        templateRepository.deleteById(id);
    }

    /**
     * 更新模板状态
     */
    @Transactional
    public TemplateVO updateTemplateStatus(Long id, Integer status) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模板不存在"));
        
        template.setStatus(status);
        Template saved = templateRepository.save(template);
        return convertToVO(saved);
    }

    /**
     * 搜索模板
     */
    public List<TemplateVO> searchTemplates(String keyword, Integer status, String category) {
        List<Template> templates = templateRepository.searchTemplates(keyword, status, category);
        return templates.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private TemplateVO convertToVO(Template template) {
        TemplateVO vo = new TemplateVO();
        BeanUtils.copyProperties(template, vo);

        // 从 styleConfig 解析 templateImages（兼容旧数据：仅 previewImage）
        vo.setTemplateImages(extractTemplateImages(template.getStyleConfig(), template.getPreviewImage()));
        
        // 设置状态文本
        if (template.getStatus() != null) {
            vo.setStatusText(template.getStatus() == 1 ? "启用" : "禁用");
        }
        
        // 设置分类文本
        if (template.getCategory() != null) {
            String categoryText = getCategoryText(template.getCategory());
            vo.setCategoryText(categoryText);
        }
        
        // 设置创建者名称
        if (template.getCreatedBy() != null) {
            try {
                var userInfo = userService.getUserInfo(template.getCreatedBy());
                vo.setCreatedByName(userInfo.getUsername());
            } catch (Exception e) {
                vo.setCreatedByName("未知");
            }
        }
        
        return vo;
    }

    private List<String> extractTemplateImages(String styleConfig, String previewImage) {
        // 优先使用 styleConfig 中的 templateImages
        if (styleConfig != null && !styleConfig.isBlank()) {
            try {
                Map<String, Object> map = objectMapper.readValue(styleConfig, new TypeReference<Map<String, Object>>() {});
                Object imagesObj = map.get("templateImages");
                if (imagesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> rawList = (List<Object>) imagesObj;
                    List<String> urls = rawList.stream()
                        .filter(o -> o != null && !String.valueOf(o).isBlank())
                        .map(String::valueOf)
                        .collect(Collectors.toList());
                    if (!urls.isEmpty()) return urls;
                }
            } catch (Exception ignored) {
                // 解析失败则走兼容逻辑
            }
        }

        if (previewImage != null && !previewImage.isBlank()) {
            return Collections.singletonList(previewImage);
        }
        return Collections.emptyList();
    }

    /**
     * 获取分类文本
     */
    private String getCategoryText(String category) {
        if (category == null) return "其他";
        switch (category) {
            case "simple": return "简约风格";
            case "warm": return "温馨风格";
            case "classic": return "经典风格";
            case "modern": return "现代风格";
            case "vintage": return "复古风格";
            case "literary": return "文艺风格";
            case "other": return "其他";
            default: return "其他";
        }
    }
}


