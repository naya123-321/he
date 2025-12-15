package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.FeedbackCreateDTO;
import com.example.demo.dto.FeedbackResponseDTO;
import com.example.demo.service.FeedbackService;
import com.example.demo.service.UserService;
import com.example.demo.vo.FeedbackVO;
import com.example.demo.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserService userService;

    /**
     * 创建反馈（宠主提交）
     */
    @PostMapping("/create")
    public Result<FeedbackVO> createFeedback(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody FeedbackCreateDTO dto) {
        try {
            Long userId = parseUserIdFromToken(token);
            FeedbackVO feedbackVO = feedbackService.createFeedback(userId, dto);
            return Result.success(feedbackVO);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("提交反馈失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务人员的反馈列表
     */
    @GetMapping("/provider/list")
    public Result<PageResult<FeedbackVO>> getProviderFeedbackList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long serviceProviderId = parseUserIdFromToken(token);
            // 验证用户角色
            UserVO userInfo = userService.getUserInfo(serviceProviderId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(1)) {
                return Result.error("无权限访问，仅服务人员可查看反馈列表");
            }

            PageResult<FeedbackVO> result = feedbackService.getFeedbackListByProvider(serviceProviderId, status, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取反馈列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的反馈列表
     */
    @GetMapping("/user/list")
    public Result<PageResult<FeedbackVO>> getUserFeedbackList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long userId = parseUserIdFromToken(token);
            PageResult<FeedbackVO> result = feedbackService.getFeedbackListByUser(userId, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取反馈列表失败: " + e.getMessage());
        }
    }

    /**
     * 处理反馈（服务人员回复）
     */
    @PostMapping("/respond")
    public Result<FeedbackVO> respondToFeedback(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody FeedbackResponseDTO dto) {
        try {
            Long serviceProviderId = parseUserIdFromToken(token);
            // 验证用户角色
            UserVO userInfo = userService.getUserInfo(serviceProviderId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(1)) {
                return Result.error("无权限操作，仅服务人员可处理反馈");
            }

            FeedbackVO feedbackVO = feedbackService.respondToFeedback(serviceProviderId, dto);
            return Result.success(feedbackVO);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("处理反馈失败: " + e.getMessage());
        }
    }

    /**
     * 创建反馈（服务人员主动提交给宠主）
     */
    @PostMapping("/provider/create")
    public Result<FeedbackVO> createFeedbackByProvider(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody FeedbackCreateDTO dto) {
        try {
            Long serviceProviderId = parseUserIdFromToken(token);
            // 验证用户角色
            UserVO userInfo = userService.getUserInfo(serviceProviderId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(1)) {
                return Result.error("无权限操作，仅服务人员可主动创建反馈");
            }

            FeedbackVO feedbackVO = feedbackService.createFeedbackByProvider(serviceProviderId, dto);
            return Result.success(feedbackVO);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建反馈失败: " + e.getMessage());
        }
    }

    /**
     * 宠主回复服务人员主动发送的反馈
     */
    @PostMapping("/user/respond")
    public Result<FeedbackVO> respondToProviderFeedback(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody FeedbackResponseDTO dto) {
        try {
            Long userId = parseUserIdFromToken(token);
            FeedbackVO feedbackVO = feedbackService.respondToProviderFeedback(userId, dto);
            return Result.success(feedbackVO);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("回复失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务人员主动发送的反馈列表
     */
    @GetMapping("/provider/sent")
    public Result<PageResult<FeedbackVO>> getProviderSentFeedbackList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long serviceProviderId = parseUserIdFromToken(token);
            // 验证用户角色
            UserVO userInfo = userService.getUserInfo(serviceProviderId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(1)) {
                return Result.error("无权限访问，仅服务人员可查看发送的反馈列表");
            }

            PageResult<FeedbackVO> result = feedbackService.getProviderSentFeedbackList(serviceProviderId, pageNum, pageSize);
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取反馈列表失败: " + e.getMessage());
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

