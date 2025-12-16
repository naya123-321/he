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
     * 绑定关联订单（宠主）
     */
    @PostMapping("/bind-order/{id}")
    public Result<MemorialVO> bindOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Long orderId) {
        try {
            Long userId = parseUserIdFromToken(token);
            MemorialVO vo = memorialService.bindOrder(id, userId, orderId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 宠主提交到服务端设计
     */
    @PostMapping("/submit-design/{id}")
    public Result<MemorialVO> submitForDesign(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            Long userId = parseUserIdFromToken(token);
            MemorialVO vo = memorialService.submitForDesign(id, userId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 服务端上传设计稿（多图 + 单PDF）
     */
    @PostMapping("/provider/upload-draft/{id}")
    public Result<MemorialVO> providerUploadDraft(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "pdf", required = false) MultipartFile pdf,
            @RequestParam(value = "message", required = false) String message) {
        try {
            Long userId = parseUserIdFromToken(token);
            java.util.List<String> imageUrls = saveImages(id, "draft", images);
            String pdfUrl = savePdf(id, "draft", pdf);
            MemorialVO vo = memorialService.providerUploadDraft(id, userId, imageUrls, pdfUrl, message);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 宠主回传修改稿（多图 + 单PDF）
     */
    @PostMapping("/owner/upload-feedback/{id}")
    public Result<MemorialVO> ownerUploadFeedback(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "pdf", required = false) MultipartFile pdf,
            @RequestParam(value = "message", required = false) String message) {
        try {
            Long userId = parseUserIdFromToken(token);
            java.util.List<String> imageUrls = saveImages(id, "owner-feedback", images);
            String pdfUrl = savePdf(id, "owner-feedback", pdf);
            MemorialVO vo = memorialService.ownerUploadFeedback(id, userId, imageUrls, pdfUrl, message);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 宠主确认最终版
     */
    @PostMapping("/owner/confirm-final/{id}")
    public Result<MemorialVO> ownerConfirmFinal(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            Long userId = parseUserIdFromToken(token);
            MemorialVO vo = memorialService.ownerConfirmFinal(id, userId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 服务端上传最终版并提交管理员审核（多图 + 单PDF）
     */
    @PostMapping("/provider/upload-final/{id}")
    public Result<MemorialVO> providerUploadFinal(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "pdf", required = false) MultipartFile pdf,
            @RequestParam(value = "message", required = false) String message) {
        try {
            Long userId = parseUserIdFromToken(token);
            java.util.List<String> imageUrls = saveImages(id, "final", images);
            String pdfUrl = savePdf(id, "final", pdf);
            MemorialVO vo = memorialService.providerUploadFinalAndSubmitReview(id, userId, imageUrls, pdfUrl, message);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端审核
     */
    @PostMapping("/admin/review/{id}")
    public Result<MemorialVO> adminReview(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Boolean approved,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) Boolean isPublic) {
        try {
            Long userId = parseUserIdFromToken(token);
            MemorialVO vo = memorialService.adminReview(id, userId, approved, comment, isPublic);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分享访问（公开、且管理员已通过）
     */
    @GetMapping("/share/{token}")
    public Result<MemorialVO> getSharedMemorial(@PathVariable String token) {
        try {
            MemorialVO vo = memorialService.getSharedMemorial(token);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

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
            @RequestParam(required = false) Integer designStatus,
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
                if (designStatus != null) {
                    result = memorialService.getMemorialListByServiceProviderWithDesignStatus(
                            currentUserId, status, designStatus, petName, title, pageNum, pageSize
                    );
                } else {
                    result = memorialService.getMemorialListByServiceProvider(
                            currentUserId, status, petName, title, pageNum, pageSize
                    );
                }
            } else if (userRole != null && userRole == 2) {
                // 管理员（role=2）可以查看所有纪念册，传入null作为userId
                if (designStatus != null) {
                    result = memorialService.getMemorialListWithDesignStatus(
                            null, status, designStatus, petName, title, pageNum, pageSize
                    );
                } else {
                    result = memorialService.getMemorialList(
                            null, status, petName, title, pageNum, pageSize
                    );
                }
            } else {
                // 宠主（role=0）只能查看自己的纪念册
                if (userId == null) {
                    userId = currentUserId;
                }
                if (designStatus != null) {
                    result = memorialService.getMemorialListWithDesignStatus(
                            userId, status, designStatus, petName, title, pageNum, pageSize
                    );
                } else {
                    result = memorialService.getMemorialList(
                            userId, status, petName, title, pageNum, pageSize
                    );
                }
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

    private java.util.List<String> saveImages(Long memorialId, String stage, MultipartFile[] images) throws IOException {
        java.util.List<String> urls = new java.util.ArrayList<>();
        if (images == null || images.length == 0) return urls;

        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) continue;
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("只能上传图片文件");
            }
            String url = saveFileToUploads(memorialId, stage, file, "images");
            urls.add(url);
        }
        return urls;
    }

    private String savePdf(Long memorialId, String stage, MultipartFile pdf) throws IOException {
        if (pdf == null || pdf.isEmpty()) return null;
        String originalFilename = pdf.getOriginalFilename();
        String contentType = pdf.getContentType();
        boolean ok = (contentType != null && contentType.equalsIgnoreCase("application/pdf"))
                || (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf"));
        if (!ok) {
            throw new RuntimeException("只能上传PDF文件");
        }
        return saveFileToUploads(memorialId, stage, pdf, "pdf");
    }

    private String saveFileToUploads(Long memorialId, String stage, MultipartFile file, String type) throws IOException {
        String uploadDir = "uploads/memorial-design/" + memorialId + "/" + stage + "/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        if ("pdf".equals(type)) {
            extension = ".pdf";
        }
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(uploadDir + filename);
        Files.write(filePath, file.getBytes());
        return "/uploads/memorial-design/" + memorialId + "/" + stage + "/" + filename;
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

