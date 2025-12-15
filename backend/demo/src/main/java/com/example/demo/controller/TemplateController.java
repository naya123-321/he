package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.TemplateCreateDTO;
import com.example.demo.service.TemplateService;
import com.example.demo.service.UserService;
import com.example.demo.vo.TemplateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/template")
public class TemplateController {
    @Autowired
    private TemplateService templateService;
    
    @Autowired
    private UserService userService;

    /**
     * 获取所有模板（管理员和服务人员）
     */
    @GetMapping("/list")
    public Result<List<TemplateVO>> getTemplateList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String category) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            
            // 管理员和服务人员可以查看所有模板
            if (userInfo.getRole() == null || (userInfo.getRole() != 1 && userInfo.getRole() != 2)) {
                return Result.error("无权限访问");
            }
            
            List<TemplateVO> templates;
            if (keyword != null && !keyword.isEmpty()) {
                templates = templateService.searchTemplates(keyword, status, category);
            } else if (status != null || (category != null && !category.isEmpty())) {
                templates = templateService.searchTemplates(null, status, category);
            } else {
                templates = templateService.getAllTemplates();
            }
            
            return Result.success(templates);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取启用的模板（公开接口，用于宠主端选择）
     */
    @GetMapping("/enabled")
    public Result<List<TemplateVO>> getEnabledTemplates(
            @RequestParam(required = false) String category) {
        try {
            List<TemplateVO> templates = templateService.getEnabledTemplates(category);
            return Result.success(templates);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取模板列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取模板详情
     */
    @GetMapping("/{id}")
    public Result<TemplateVO> getTemplateById(@PathVariable Long id) {
        try {
            TemplateVO template = templateService.getTemplateById(id);
            return Result.success(template);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取模板详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建模板（管理员和服务人员）
     */
    @PostMapping("/create")
    public Result<TemplateVO> createTemplate(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody TemplateCreateDTO dto) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            
            // 管理员和服务人员可以创建模板
            if (userInfo.getRole() == null || (userInfo.getRole() != 1 && userInfo.getRole() != 2)) {
                return Result.error("无权限访问，仅管理员和服务人员可创建模板");
            }
            
            TemplateVO template = templateService.createTemplate(dto, currentUserId);
            return Result.success(template);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建模板失败: " + e.getMessage());
        }
    }

    /**
     * 更新模板（管理员和服务人员）
     */
    @PutMapping("/{id}")
    public Result<TemplateVO> updateTemplate(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestBody TemplateCreateDTO dto) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            
            // 管理员和服务人员可以更新模板
            if (userInfo.getRole() == null || (userInfo.getRole() != 1 && userInfo.getRole() != 2)) {
                return Result.error("无权限访问，仅管理员和服务人员可更新模板");
            }
            
            TemplateVO template = templateService.updateTemplate(id, dto);
            return Result.success(template);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除模板（仅管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteTemplate(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            
            // 仅管理员可以删除模板
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可删除模板");
            }
            
            templateService.deleteTemplate(id);
            return Result.success(true);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除模板失败: " + e.getMessage());
        }
    }

    /**
     * 更新模板状态（管理员和服务人员）
     */
    @PutMapping("/{id}/status")
    public Result<TemplateVO> updateTemplateStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Integer status) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            
            // 管理员和服务人员可以更新模板状态
            if (userInfo.getRole() == null || (userInfo.getRole() != 1 && userInfo.getRole() != 2)) {
                return Result.error("无权限访问，仅管理员和服务人员可更新模板状态");
            }
            
            TemplateVO template = templateService.updateTemplateStatus(id, status);
            return Result.success(template);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新模板状态失败: " + e.getMessage());
        }
    }

    /**
     * 上传模板预览图
     */
    @PostMapping("/upload-preview")
    public Result<String> uploadPreviewImage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam("file") MultipartFile file) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            
            // 管理员和服务人员可以上传预览图
            if (userInfo.getRole() == null || (userInfo.getRole() != 1 && userInfo.getRole() != 2)) {
                return Result.error("无权限访问");
            }
            
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }
            
            // 保存文件
            String uploadDir = "uploads/templates/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = Paths.get(uploadDir + filename);
            
            Files.write(filePath, file.getBytes());
            
            // 返回相对路径
            String imageUrl = "/uploads/templates/" + filename;
            return Result.success(imageUrl);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 从token中解析userId
     */
    private Long parseUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("未登录");
        }
        String tokenValue = token.substring(7);
        if (tokenValue.startsWith("token_")) {
            String[] parts = tokenValue.split("_");
            if (parts.length >= 2) {
                return Long.parseLong(parts[1]);
            }
        }
        throw new RuntimeException("无效的token");
    }
}


