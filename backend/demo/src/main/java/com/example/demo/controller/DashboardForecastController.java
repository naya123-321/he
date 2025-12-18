package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.service.OrderForecastService;
import com.example.demo.service.UserService;
import com.example.demo.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardForecastController {

    @Autowired
    private OrderForecastService orderForecastService;

    @Autowired
    private UserService userService;

    /**
     * 未来订单量预测（ARIMA：MySQL -> Python -> JSON -> API）
     */
    @GetMapping("/order-forecast")
    public Result<Map<String, Object>> getOrderForecast(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "3") Integer days,
            @RequestParam(defaultValue = "60") Integer historyDays,
            @RequestParam(required = false, defaultValue = "false") Boolean force
    ) {
        try {
            Long userId = parseUserIdFromToken(token);
            UserVO currentUser = userService.getUserInfo(userId);
            if (currentUser.getRole() == null || !currentUser.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可查看预测数据");
            }
            Map<String, Object> data = orderForecastService.getOrderForecast(days, historyDays, force);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
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



