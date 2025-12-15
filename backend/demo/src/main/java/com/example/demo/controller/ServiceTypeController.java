package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.service.ServiceTypeService;
import com.example.demo.service.UserService;
import com.example.demo.vo.ServiceTypeVO;
import com.example.demo.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-type")
public class ServiceTypeController {
    @Autowired
    private ServiceTypeService serviceTypeService;
    
    @Autowired
    private UserService userService;

    /**
     * 获取所有服务类型（包括禁用的，管理员使用）
     */
    @GetMapping("/list")
    public Result<List<ServiceTypeVO>> getAllServiceTypes(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 验证管理员权限
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    String tokenValue = token.substring(7);
                    if (tokenValue.startsWith("token_")) {
                        String[] parts = tokenValue.split("_");
                        if (parts.length >= 2) {
                            Long userId = Long.parseLong(parts[1]);
                            UserVO userInfo = userService.getUserInfo(userId);
                            if (userInfo.getRole() != null && userInfo.getRole() == 2) {
                                // 管理员可以查看所有服务类型（包括禁用的）
                                List<ServiceTypeVO> serviceTypes = serviceTypeService.getAllServiceTypes();
                                return Result.success(serviceTypes);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略权限验证错误，返回启用的服务类型
                }
            }
            // 非管理员或未登录用户只能查看启用的服务类型
            List<ServiceTypeVO> serviceTypes = serviceTypeService.getEnabledServiceTypes();
            return Result.success(serviceTypes);
        } catch (Exception e) {
            return Result.error("获取服务类型失败: " + e.getMessage());
        }
    }

    /**
     * 获取启用的服务类型（公开接口）
     */
    @GetMapping("/enabled")
    public Result<List<ServiceTypeVO>> getEnabledServiceTypes() {
        try {
            List<ServiceTypeVO> serviceTypes = serviceTypeService.getEnabledServiceTypes();
            return Result.success(serviceTypes);
        } catch (Exception e) {
            return Result.error("获取服务类型失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取服务类型
     */
    @GetMapping("/{id}")
    public Result<ServiceTypeVO> getServiceTypeById(@PathVariable Long id) {
        try {
            ServiceTypeVO serviceType = serviceTypeService.getServiceTypeById(id);
            return Result.success(serviceType);
        } catch (Exception e) {
            return Result.error("获取服务类型失败: " + e.getMessage());
        }
    }

    /**
     * 创建服务类型（仅管理员）
     */
    @PostMapping("/create")
    public Result<ServiceTypeVO> createServiceType(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody ServiceTypeVO vo) {
        try {
            // 验证管理员权限
            Long userId = parseUserIdFromToken(token);
            UserVO userInfo = userService.getUserInfo(userId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可创建服务类型");
            }
            
            ServiceTypeVO created = serviceTypeService.createServiceType(vo);
            return Result.success(created);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建服务类型失败: " + e.getMessage());
        }
    }

    /**
     * 更新服务类型（仅管理员）
     */
    @PutMapping("/{id}")
    public Result<ServiceTypeVO> updateServiceType(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestBody ServiceTypeVO vo) {
        try {
            // 验证管理员权限
            Long userId = parseUserIdFromToken(token);
            UserVO userInfo = userService.getUserInfo(userId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可更新服务类型");
            }
            
            ServiceTypeVO updated = serviceTypeService.updateServiceType(id, vo);
            return Result.success(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新服务类型失败: " + e.getMessage());
        }
    }

    /**
     * 删除服务类型（仅管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteServiceType(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            // 验证管理员权限
            Long userId = parseUserIdFromToken(token);
            UserVO userInfo = userService.getUserInfo(userId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可删除服务类型");
            }
            
            boolean success = serviceTypeService.deleteServiceType(id);
            return Result.success(success);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除服务类型失败: " + e.getMessage());
        }
    }

    /**
     * 启用/禁用服务类型（仅管理员）
     */
    @PutMapping("/{id}/status")
    public Result<ServiceTypeVO> updateStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Integer status) {
        try {
            // 验证管理员权限
            Long userId = parseUserIdFromToken(token);
            UserVO userInfo = userService.getUserInfo(userId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可修改服务类型状态");
            }
            
            ServiceTypeVO updated = serviceTypeService.updateStatus(id, status);
            return Result.success(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新服务类型状态失败: " + e.getMessage());
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


