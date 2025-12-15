package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.RegisterDTO;
import com.example.demo.service.UserService;
import com.example.demo.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO loginDTO) {
        try {
            String token = userService.login(loginDTO);
            return Result.success(token);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody RegisterDTO registerDTO) {
        try {
            boolean success = userService.register(registerDTO);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check-username")
    public Result<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.checkUsername(username);
        return Result.success(exists);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user-info")
    public Result<UserVO> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 简单解析token获取userId，实际应该使用JWT
            Long userId = parseUserIdFromToken(token);
            UserVO userVO = userService.getUserInfo(userId);
            return Result.success(userVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证用户身份
     */
    @PostMapping("/verify-identity")
    public Result<Boolean> verifyIdentity(@RequestBody RegisterDTO dto) {
        try {
            boolean verified = userService.verifyIdentity(dto.getUsername(), dto.getEmail());
            return Result.success(verified);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送验证码（模拟）
     */
    @PostMapping("/send-verification-code")
    public Result<Boolean> sendVerificationCode(@RequestBody RegisterDTO dto) {
        // 模拟发送验证码
        return Result.success(true);
    }

    /**
     * 验证邮箱验证码（模拟）
     */
    @PostMapping("/verify-code")
    public Result<Boolean> verifyEmailCode(@RequestBody RegisterDTO dto) {
        // 模拟验证验证码
        return Result.success(true);
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<Boolean> resetPassword(@RequestBody RegisterDTO dto) {
        try {
            boolean success = userService.resetPassword(dto.getUsername(), dto.getPassword());
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户列表（管理员功能）
     */
    @GetMapping("/user-list")
    public Result<PageResult<UserVO>> getUserList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            // 验证管理员权限（简单实现，实际应该检查用户角色）
            Long userId = parseUserIdFromToken(token);
            UserVO currentUser = userService.getUserInfo(userId);
            if (currentUser.getRole() == null || !currentUser.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可查看用户列表");
            }
            
            PageResult<UserVO> result = userService.getUserList(role, keyword, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            // 记录详细错误信息
            e.printStackTrace();
            return Result.error("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息（管理员功能）
     */
    @PutMapping("/user/{id}")
    public Result<UserVO> updateUser(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestBody UserVO userVO) {
        try {
            // 验证管理员权限
            Long currentUserId = parseUserIdFromToken(token);
            UserVO currentUser = userService.getUserInfo(currentUserId);
            if (currentUser.getRole() == null || !currentUser.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可更新用户信息");
            }
            
            UserVO updated = userService.updateUser(id, userVO);
            return Result.success(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户（管理员功能）
     */
    @DeleteMapping("/user/{id}")
    public Result<Boolean> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            // 验证管理员权限
            Long currentUserId = parseUserIdFromToken(token);
            UserVO currentUser = userService.getUserInfo(currentUserId);
            if (currentUser.getRole() == null || !currentUser.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可删除用户");
            }
            
            boolean success = userService.deleteUser(id);
            return Result.success(success);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 管理员重置用户密码
     */
    @PostMapping("/user/{id}/reset-password")
    public Result<Boolean> adminResetPassword(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        try {
            // 验证管理员权限
            Long currentUserId = parseUserIdFromToken(token);
            UserVO currentUser = userService.getUserInfo(currentUserId);
            if (currentUser.getRole() == null || !currentUser.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可重置用户密码");
            }
            
            String newPassword = requestBody.get("newPassword");
            if (newPassword == null || newPassword.isEmpty()) {
                return Result.error("新密码不能为空");
            }
            
            boolean success = userService.adminResetPassword(id, newPassword);
            return Result.success(success);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("重置密码失败: " + e.getMessage());
        }
    }

    /**
     * 用户更新自己的信息
     */
    @PutMapping("/user/update-info")
    public Result<UserVO> updateUserInfo(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody UserVO userVO) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            UserVO updated = userService.updateUserInfo(currentUserId, userVO);
            return Result.success(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 从token中解析userId（简单实现）
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


