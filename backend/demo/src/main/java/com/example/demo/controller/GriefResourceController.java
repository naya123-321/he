package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.GriefResourceCreateDTO;
import com.example.demo.service.GriefResourceService;
import com.example.demo.service.UserService;
import com.example.demo.vo.GriefResourceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grief-resource")
public class GriefResourceController {
    @Autowired
    private GriefResourceService griefResourceService;

    @Autowired
    private UserService userService;

    /**
     * 宠主端：获取启用的资源（公开接口）
     */
    @GetMapping("/enabled")
    public Result<List<GriefResourceVO>> getEnabled(@RequestParam(required = false) String type) {
        try {
            return Result.success(griefResourceService.getEnabledResources(type));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端：分页列表（管理员）
     */
    @GetMapping("/list")
    public Result<PageResult<GriefResourceVO>> list(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || userInfo.getRole() != 2) {
                return Result.error("无权限访问");
            }

            PageResult<GriefResourceVO> result = griefResourceService.getResourceList(type, status, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 详情（公开，便于前端弹窗查看）
     */
    @GetMapping("/detail/{id}")
    public Result<GriefResourceVO> detail(@PathVariable Long id) {
        try {
            return Result.success(griefResourceService.getDetail(id));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建（管理员）
     */
    @PostMapping("/create")
    public Result<GriefResourceVO> create(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody GriefResourceCreateDTO dto
    ) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || userInfo.getRole() != 2) {
                return Result.error("无权限访问");
            }
            return Result.success(griefResourceService.create(dto));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新（管理员）
     */
    @PutMapping("/{id}")
    public Result<GriefResourceVO> update(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestBody GriefResourceCreateDTO dto
    ) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || userInfo.getRole() != 2) {
                return Result.error("无权限访问");
            }
            return Result.success(griefResourceService.update(id, dto));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除（管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id
    ) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || userInfo.getRole() != 2) {
                return Result.error("无权限访问");
            }
            griefResourceService.delete(id);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新状态（管理员）
     */
    @PutMapping("/{id}/status")
    public Result<GriefResourceVO> updateStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Integer status
    ) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || userInfo.getRole() != 2) {
                return Result.error("无权限访问");
            }
            return Result.success(griefResourceService.updateStatus(id, status));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上传资源文件（封面图片/视频文件）（管理员）
     * 返回可访问的 URL：/uploads/grief/xxx.ext
     */
    @PostMapping("/upload")
    public Result<String> upload(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || userInfo.getRole() != 2) {
                return Result.error("无权限访问");
            }

            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String contentType = file.getContentType() != null ? file.getContentType() : "";
            boolean isImage = contentType.startsWith("image/");
            boolean isVideo = contentType.startsWith("video/");
            if (!isImage && !isVideo) {
                return Result.error("仅支持图片或视频文件");
            }

            // 简单大小限制：图片 10MB，视频 200MB（与前端校验一致）
            long size = file.getSize();
            if (isImage && size > 10L * 1024 * 1024) {
                return Result.error("图片不能超过 10MB");
            }
            if (isVideo && size > 200L * 1024 * 1024) {
                return Result.error("视频不能超过 200MB");
            }

            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = "";
            int dot = original.lastIndexOf(".");
            if (dot >= 0) {
                ext = original.substring(dot);
            }

            String subDir = isVideo ? "videos" : "images";
            Path uploadDir = Paths.get("uploads", "grief", subDir);
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path dest = uploadDir.resolve(filename);
            // Windows + Tomcat 下 transferTo 可能触发路径/重命名问题，使用流拷贝更稳
            Files.createDirectories(dest.getParent());
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/grief/" + subDir + "/" + filename;
            return Result.success(url);
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



