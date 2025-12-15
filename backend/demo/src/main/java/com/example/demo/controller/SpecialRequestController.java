package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.SpecialRequestCreateDTO;
import com.example.demo.dto.SpecialRequestReviewDTO;
import com.example.demo.service.SpecialRequestService;
import com.example.demo.service.UserService;
import com.example.demo.vo.SpecialRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/special-request")
public class SpecialRequestController {
    @Autowired
    private SpecialRequestService specialRequestService;
    
    @Autowired
    private UserService userService;

    /**
     * 创建特殊需求
     */
    @PostMapping("/create")
    public Result<SpecialRequestVO> createSpecialRequest(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SpecialRequestCreateDTO dto) {
        try {
            Long userId = parseUserIdFromToken(token);
            SpecialRequestVO vo = specialRequestService.createSpecialRequest(userId, dto);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取特殊需求列表（管理员）
     */
    @GetMapping("/list")
    public Result<PageResult<SpecialRequestVO>> getSpecialRequestList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            Integer userRole = userInfo.getRole();
            
            // 只有管理员可以查看所有特殊需求
            if (userRole == null || userRole != 2) {
                return Result.error("无权限访问");
            }
            
            PageResult<SpecialRequestVO> result = specialRequestService.getSpecialRequestList(
                status, pageNum, pageSize
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户的特殊需求列表
     */
    @GetMapping("/user/list")
    public Result<PageResult<SpecialRequestVO>> getUserSpecialRequestList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long userId = parseUserIdFromToken(token);
            PageResult<SpecialRequestVO> result = specialRequestService.getUserSpecialRequestList(
                userId, pageNum, pageSize
            );
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取特殊需求详情
     */
    @GetMapping("/detail/{id}")
    public Result<SpecialRequestVO> getSpecialRequestDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        try {
            SpecialRequestVO vo = specialRequestService.getSpecialRequestDetail(id);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 审核特殊需求
     */
    @PostMapping("/review")
    public Result<SpecialRequestVO> reviewSpecialRequest(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody SpecialRequestReviewDTO dto) {
        try {
            Long adminId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(adminId);
            Integer userRole = userInfo.getRole();
            
            // 只有管理员可以审核
            if (userRole == null || userRole != 2) {
                return Result.error("无权限操作");
            }
            
            SpecialRequestVO vo = specialRequestService.reviewSpecialRequest(adminId, dto);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据订单ID获取已通过审核的特殊需求
     */
    @GetMapping("/by-order/{orderId}")
    public Result<SpecialRequestVO> getApprovedSpecialRequestByOrderId(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long orderId) {
        try {
            SpecialRequestVO vo = specialRequestService.getApprovedSpecialRequestByOrderId(orderId);
            if (vo == null) {
                return Result.success(null);
            }
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
