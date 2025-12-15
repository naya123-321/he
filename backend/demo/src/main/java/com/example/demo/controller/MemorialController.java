package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.MemorialCreateDTO;
import com.example.demo.service.MemorialService;
import com.example.demo.vo.MemorialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/memorial")
public class MemorialController {
    @Autowired
    private MemorialService memorialService;
    
    @Autowired
    private com.example.demo.service.UserService userService;

    /**
     * 创建纪念册
     */
    @PostMapping("/create")
    public Result<MemorialVO> createMemorial(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody MemorialCreateDTO dto) {
        try {
            Long userId = parseUserIdFromToken(token);
            MemorialVO memorialVO = memorialService.createMemorial(userId, dto);
            return Result.success(memorialVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 保存内容
     */
    @PostMapping("/save-content")
    public Result<Boolean> saveContent(@RequestBody MemorialContentDTO dto) {
        try {
            boolean success = memorialService.saveContent(
                dto.getMemorialId(), dto.getContentData(), dto.getStyleConfig()
            );
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 自动保存
     */
    @PostMapping("/auto-save/{id}")
    public Result<Boolean> autoSave(@PathVariable Long id, @RequestBody Map<String, Object> contentData) {
        try {
            boolean success = memorialService.saveContent(id, contentData, null);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发布纪念册
     */
    @PostMapping("/publish")
    public Result<MemorialVO> publishMemorial(@RequestBody MemorialPublishDTO dto) {
        try {
            MemorialVO memorialVO = memorialService.publishMemorial(dto.getMemorialId(), dto.getIsPublic());
            return Result.success(memorialVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取纪念册列表
     */
    @GetMapping("/list")
    public Result<PageResult<MemorialVO>> getMemorialList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String petName,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            Integer userRole = userInfo.getRole();
            
            PageResult<MemorialVO> result;
            
            // 如果是服务人员（role=1），获取与他们服务的订单相关的纪念册
            if (userRole != null && userRole == 1) {
                result = memorialService.getMemorialListByServiceProvider(
                    currentUserId, status, petName, title, pageNum, pageSize
                );
            } else if (userRole != null && userRole == 2) {
                // 管理员（role=2）可以查看所有纪念册，传入null作为userId
                result = memorialService.getMemorialList(
                    null, status, petName, title, pageNum, pageSize
                );
            } else {
                // 宠主（role=0）只能查看自己的纪念册
                if (userId == null) {
                    userId = currentUserId;
                }
                result = memorialService.getMemorialList(
                    userId, status, petName, title, pageNum, pageSize
                );
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取纪念册详情
     */
    @GetMapping("/detail/{id}")
    public Result<MemorialVO> getMemorialDetail(@PathVariable Long id) {
        try {
            MemorialVO memorialVO = memorialService.getMemorialDetail(id);
            return Result.success(memorialVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据订单ID获取纪念册
     */
    @GetMapping("/by-order/{orderId}")
    public Result<MemorialVO> getMemorialByOrderId(@PathVariable Long orderId) {
        try {
            MemorialVO memorialVO = memorialService.getMemorialByOrderId(orderId);
            if (memorialVO == null) {
                return Result.success(null);
            }
            return Result.success(memorialVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * ?????????/?????
     */
    @PostMapping("/upload-image/{memorialId}")
    public Result<String> uploadImage(@PathVariable String memorialId, @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("??????");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("?????????");
            }

            String folderName;
            try {
                Long parsedId = Long.parseLong(memorialId);
                folderName = parsedId != null && parsedId > 0 ? parsedId.toString() : "temp";
            } catch (NumberFormatException e) {
                folderName = "temp";
            }

            String uploadDir = "uploads/memorial/" + folderName + "/";
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

            String imageUrl = "/uploads/memorial/" + folderName + "/" + filename;
            return Result.success(imageUrl);
        } catch (IOException e) {
            return Result.error("????: " + e.getMessage());
        } catch (Exception e) {
            return Result.error("????: " + e.getMessage());
        }
    }

    /**
     * 生成PDF（简化实现）
     */
    @PostMapping("/generate-pdf/{id}")
    public Result<String> generatePdf(@PathVariable Long id) {
        // 简化实现，返回模拟URL
        return Result.success("/pdfs/memorial_" + id + ".pdf");
    }

    /**
     * 点赞
     */
    @PostMapping("/like/{id}")
    public Result<Boolean> likeMemorial(@PathVariable Long id) {
        try {
            boolean success = memorialService.likeMemorial(id);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消点赞
     */
    @PostMapping("/unlike/{id}")
    public Result<Boolean> unlikeMemorial(@PathVariable Long id) {
        try {
            boolean success = memorialService.unlikeMemorial(id);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 收藏（简化实现）
     */
    @PostMapping("/favorite/{id}")
    public Result<Boolean> favoriteMemorial(@PathVariable Long id) {
        return Result.success(true);
    }

    /**
     * 取消收藏（简化实现）
     */
    @PostMapping("/unfavorite/{id}")
    public Result<Boolean> unfavoriteMemorial(@PathVariable Long id) {
        return Result.success(true);
    }

    /**
     * 删除纪念册
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteMemorial(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            Long userId = parseUserIdFromToken(token);
            boolean success = memorialService.deleteMemorial(id, userId);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("删除失败：纪念册不存在或无权删除");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
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

    /**
     * 纪念册内容DTO
     */
    public static class MemorialContentDTO {
        private Long memorialId;
        private Object contentData;
        private Object styleConfig;

        public Long getMemorialId() {
            return memorialId;
        }

        public void setMemorialId(Long memorialId) {
            this.memorialId = memorialId;
        }

        public Object getContentData() {
            return contentData;
        }

        public void setContentData(Object contentData) {
            this.contentData = contentData;
        }

        public Object getStyleConfig() {
            return styleConfig;
        }

        public void setStyleConfig(Object styleConfig) {
            this.styleConfig = styleConfig;
        }
    }

    /**
     * 发布DTO
     */
    public static class MemorialPublishDTO {
        private Long memorialId;
        private Boolean isPublic;
        private String shareMessage;

        public Long getMemorialId() {
            return memorialId;
        }

        public void setMemorialId(Long memorialId) {
            this.memorialId = memorialId;
        }

        public Boolean getIsPublic() {
            return isPublic;
        }

        public void setIsPublic(Boolean isPublic) {
            this.isPublic = isPublic;
        }

        public String getShareMessage() {
            return shareMessage;
        }

        public void setShareMessage(String shareMessage) {
            this.shareMessage = shareMessage;
        }
    }
}

