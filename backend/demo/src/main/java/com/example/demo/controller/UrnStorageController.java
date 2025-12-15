package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.UrnStorageCreateDTO;
import com.example.demo.service.UrnStorageService;
import com.example.demo.service.UserService;
import com.example.demo.vo.UrnStorageVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urn-storage")
public class UrnStorageController {
    @Autowired
    private UrnStorageService urnStorageService;
    
    @Autowired
    private UserService userService;

    /**
     * 创建骨灰寄存记录
     */
    @PostMapping("/create")
    public Result<UrnStorageVO> createUrnStorage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody UrnStorageCreateDTO dto) {
        try {
            Long userId = parseUserIdFromToken(token);
            UrnStorageVO vo = urnStorageService.createUrnStorage(userId, dto);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取骨灰寄存列表
     */
    @GetMapping("/list")
    public Result<PageResult<UrnStorageVO>> getUrnStorageList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String petName,
            @RequestParam(required = false) String urnNo,
            @RequestParam(required = false) Long orderId, // 添加 orderId 参数
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            Integer userRole = userInfo.getRole();
            
            // 如果是管理员或服务人员，可以查看所有记录
            // 如果是宠主，只能查看自己的记录
            if (userRole == null || userRole == 0) {
                userId = currentUserId;
            } else {
                // 管理员和服务人员可以查看所有，userId保持为null
            }
            
            PageResult<UrnStorageVO> result = urnStorageService.getUrnStorageList(
                userId, status, petName, urnNo, orderId, pageNum, pageSize
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取骨灰寄存详情
     */
    @GetMapping("/detail/{id}")
    public Result<UrnStorageVO> getUrnStorageDetail(@PathVariable Long id) {
        try {
            UrnStorageVO vo = urnStorageService.getUrnStorageDetail(id);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取回骨灰
     */
    @PostMapping("/retrieve/{id}")
    public Result<UrnStorageVO> retrieveUrnStorage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            Long userId = parseUserIdFromToken(token);
            UrnStorageVO vo = urnStorageService.retrieveUrnStorage(id, userId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新骨灰寄存信息
     */
    @PutMapping("/{id}")
    public Result<UrnStorageVO> updateUrnStorage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestBody UrnStorageCreateDTO dto) {
        try {
            Long userId = parseUserIdFromToken(token);
            UrnStorageVO vo = urnStorageService.updateUrnStorage(id, dto, userId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除骨灰寄存记录
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUrnStorage(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            Long userId = parseUserIdFromToken(token);
            boolean success = urnStorageService.deleteUrnStorage(id, userId);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审批骨灰寄存请求
     */
    @PostMapping("/approve/{id}")
    public Result<UrnStorageVO> approveUrnStorageRequest(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Boolean approved) {
        try {
            Long userId = parseUserIdFromToken(token);
            UrnStorageVO vo = urnStorageService.approveUrnStorageRequest(id, userId, approved);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从token中解析用户ID
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

