package com.example.demo.controller;

import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.dto.OrderCreateDTO;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import com.example.demo.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserService userService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public Result<OrderVO> createOrder(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody OrderCreateDTO dto) {
        try {
            Long userId = parseUserIdFromToken(token);
            OrderVO orderVO = orderService.createOrder(userId, dto);
            return Result.success(orderVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单列表
     */
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> getOrderList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long serviceProviderId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String petName,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            
            // 获取当前用户信息，判断角色
            Integer userRole = null;
            try {
                var userInfo = userService.getUserInfo(currentUserId);
                userRole = userInfo.getRole();
            } catch (Exception e) {
                // 忽略获取用户信息失败的情况
            }
            
            // 如果是服务人员（role=1）或管理员（role=2）且没有指定userId，允许查询所有订单（userId=null）
            // 如果是宠主（role=0）且没有指定userId，只查询自己的订单
            if (userId == null) {
                if (userRole != null && (userRole == 1 || userRole == 2)) {
                    // 服务人员或管理员：允许查询所有订单，userId保持为null
                    userId = null;
                } else {
                    // 宠主：只查询自己的订单
                    userId = currentUserId;
                }
            }
            
            PageResult<OrderVO> result = orderService.getOrderList(
                userId, serviceProviderId, status, petName, orderNo, startDate, endDate, pageNum, pageSize
            );
            
            // 如果当前用户是服务人员，检查订单列表中是否有状态>=1但未分配服务人员的订单，自动分配
            if (userRole != null && userRole == 1 && result != null && result.getRecords() != null) {
                for (OrderVO orderVO : result.getRecords()) {
                    if (orderVO.getStatus() != null && orderVO.getStatus() >= 1 && orderVO.getServiceProviderId() == null) {
                        try {
                            boolean assigned = orderService.assignServiceProvider(orderVO.getId(), currentUserId);
                            if (assigned) {
                                System.out.println("自动分配服务人员ID: " + currentUserId + " 给订单: " + orderVO.getId() + " (通过订单列表触发)");
                                // 更新订单VO中的服务人员信息
                                var providerInfo = userService.getUserInfo(currentUserId);
                                orderVO.setServiceProviderId(currentUserId);
                                orderVO.setServiceProviderName(providerInfo.getUsername());
                                orderVO.setServiceProviderPhone(providerInfo.getPhone());
                            }
                        } catch (Exception e) {
                            System.err.println("自动分配服务人员失败，订单ID: " + orderVO.getId() + ", 错误: " + e.getMessage());
                            // 忽略错误，继续处理其他订单
                        }
                    }
                }
            }
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/detail/{id}")
    public Result<OrderVO> getOrderDetail(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            OrderVO orderVO = orderService.getOrderDetail(id);
            
            // 如果订单状态>=1但还没有分配服务人员，且当前用户是服务人员，自动分配
            if (orderVO.getStatus() != null && orderVO.getStatus() >= 1 && orderVO.getServiceProviderId() == null) {
                try {
                    var userInfo = userService.getUserInfo(currentUserId);
                    if (userInfo.getRole() != null && userInfo.getRole() == 1) {
                        // 当前用户是服务人员，自动分配给自己
                        boolean assigned = orderService.assignServiceProvider(id, currentUserId);
                        if (assigned) {
                            System.out.println("自动分配服务人员ID: " + currentUserId + " 给订单: " + id + " (通过获取订单详情触发)");
                            // 重新获取订单详情，确保返回最新的服务人员信息
                            orderVO = orderService.getOrderDetail(id);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("自动分配服务人员失败: " + e.getMessage());
                    // 忽略错误，继续返回订单详情
                }
            }
            
            return Result.success(orderVO);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel/{id}")
    public Result<Boolean> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "") String reason) {
        try {
            boolean success = orderService.cancelOrder(id, reason);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新订单状态
     */
    @PostMapping("/update-status")
    public Result<Boolean> updateOrderStatus(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody OrderStatusUpdateDTO dto) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            Integer userRole = userInfo.getRole();
            
            // 检查订单是否存在并获取订单信息
            var orderVO = orderService.getOrderDetail(dto.getOrderId());
            if (orderVO == null) {
                return Result.error("订单不存在");
            }
            
            // 权限检查：只有管理员或订单的服务人员可以操作
            // 管理员（role=2）可以操作所有订单
            // 服务人员（role=1）只能操作分配给自己的订单或未分配的订单
            if (userRole != null && userRole == 1) {
                // 服务人员：只能操作未分配的订单或分配给自己的订单
                if (orderVO.getServiceProviderId() != null && !orderVO.getServiceProviderId().equals(currentUserId)) {
                    return Result.error("无权限操作，该订单已分配给其他服务人员");
                }
            } else if (userRole == null || userRole != 2) {
                // 非管理员且非服务人员（如宠主）不能更新订单状态
                return Result.error("无权限操作订单状态");
            }
            
            // 如果目标状态是"已确认"（status=1）或"服务中"（status=2），且当前用户是服务人员，自动分配给自己
            Long serviceProviderId = null;
            if (dto.getTargetStatus() != null && (dto.getTargetStatus() == 1 || dto.getTargetStatus() == 2)) {
                if (userRole != null && userRole == 1) {
                    // 当前用户是服务人员，自动分配给自己
                    serviceProviderId = currentUserId;
                    System.out.println("自动分配服务人员ID: " + serviceProviderId + " 给订单: " + dto.getOrderId());
                }
            }
            
            // 如果订单状态>=1但还没有分配服务人员，且当前用户是服务人员，强制分配
            // 注意：这里直接通过service层检查订单，避免循环调用
            if (serviceProviderId == null && dto.getTargetStatus() != null && dto.getTargetStatus() >= 1) {
                if (userRole != null && userRole == 1) {
                    // 通过service层检查订单是否已有服务人员（避免循环调用）
                    serviceProviderId = orderService.checkAndAssignProvider(dto.getOrderId(), currentUserId);
                    if (serviceProviderId != null) {
                        System.out.println("强制分配服务人员ID: " + serviceProviderId + " 给订单: " + dto.getOrderId() + " (订单状态: " + dto.getTargetStatus() + ")");
                    }
                }
            }
            
            boolean success = orderService.updateOrderStatus(dto.getOrderId(), dto.getTargetStatus(), dto.getRemark(), serviceProviderId);
            return Result.success(success);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分配服务人员
     */
    @PostMapping("/assign-provider")
    public Result<Boolean> assignServiceProvider(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody AssignProviderDTO dto) {
        try {
            // 验证权限（服务人员或管理员可以分配）
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            Integer userRole = userInfo.getRole();
            
            if (userRole == null || (userRole != 1 && userRole != 2)) {
                return Result.error("无权限分配服务人员");
            }
            
            boolean success = orderService.assignServiceProvider(dto.getOrderId(), dto.getServiceProviderId());
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 完成订单并评价
     */
    @PostMapping("/complete/{id}")
    public Result<Boolean> completeOrder(
            @PathVariable Long id,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String review) {
        try {
            boolean success = orderService.completeOrder(id, rating, review);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 宠主提交满意度评价（仅已完成订单，不改变订单状态）
     */
    @PostMapping("/review/{id}")
    public Result<Boolean> submitReview(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam(required = false) String review
    ) {
        try {
            Long userId = parseUserIdFromToken(token);
            boolean success = orderService.submitReview(id, userId, rating, review);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加服务记录
     */
    @PostMapping("/add-note/{id}")
    public Result<Boolean> addServiceNote(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id,
            @RequestParam String notes) {
        try {
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            Integer userRole = userInfo.getRole();
            
            // 检查订单是否存在并获取订单信息
            var orderVO = orderService.getOrderDetail(id);
            if (orderVO == null) {
                return Result.error("订单不存在");
            }
            
            // 权限检查：只有管理员或订单的服务人员可以添加服务记录
            if (userRole != null && userRole == 1) {
                // 服务人员：只能操作分配给自己的订单
                if (orderVO.getServiceProviderId() == null || !orderVO.getServiceProviderId().equals(currentUserId)) {
                    return Result.error("无权限操作，该订单未分配给您");
                }
            } else if (userRole == null || userRole != 2) {
                // 非管理员且非服务人员不能添加服务记录
                return Result.error("无权限添加服务记录");
            }
            
            // updateOrderStatus需要4个参数：orderId, targetStatus, remark, serviceProviderId
            // 添加服务记录时不更新状态，也不分配服务人员，所以targetStatus和serviceProviderId都传null
            boolean success = orderService.updateOrderStatus(id, null, notes, null);
            return Result.success(success);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取订单状态日志
     */
    @GetMapping("/status-logs/{id}")
    public Result<Object[]> getStatusLogs(@PathVariable Long id) {
        // 简化实现，返回空数组
        return Result.success(new Object[0]);
    }

    /**
     * 获取满意度统计数据
     */
    @GetMapping("/satisfaction-stats")
    public Result<Map<String, Object>> getSatisfactionStats(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 验证管理员权限
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可查看满意度统计");
            }
            
            Map<String, Object> stats = orderService.getSatisfactionStats();
            return Result.success(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取满意度统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取满意度趋势数据
     */
    @GetMapping("/satisfaction-trend")
    public Result<Map<String, Object>> getSatisfactionTrend(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        try {
            // 验证管理员权限
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可查看满意度趋势");
            }
            
            Map<String, Object> trend = orderService.getSatisfactionTrend(days);
            return Result.success(trend);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取满意度趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取服务类型分布数据
     */
    @GetMapping("/service-type-distribution")
    public Result<Map<String, Object>> getServiceTypeDistribution(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // 验证管理员权限
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可查看服务类型分布");
            }
            
            Map<String, Object> distribution = orderService.getServiceTypeDistribution();
            return Result.success(distribution);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取服务类型分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单趋势数据
     */
    @GetMapping("/order-trend")
    public Result<Map<String, Object>> getOrderTrend(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(required = false, defaultValue = "7") Integer days) {
        try {
            // 验证管理员权限
            Long currentUserId = parseUserIdFromToken(token);
            var userInfo = userService.getUserInfo(currentUserId);
            if (userInfo.getRole() == null || !userInfo.getRole().equals(2)) {
                return Result.error("无权限访问，仅管理员可查看订单趋势");
            }
            
            Map<String, Object> trend = orderService.getOrderTrend(days);
            return Result.success(trend);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取订单趋势失败: " + e.getMessage());
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
     * 订单状态更新DTO
     */
    public static class OrderStatusUpdateDTO {
        private Long orderId;
        private Integer targetStatus;
        private String remark;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Integer getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(Integer targetStatus) {
            this.targetStatus = targetStatus;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    /**
     * 分配服务人员DTO
     */
    public static class AssignProviderDTO {
        private Long orderId;
        private Long serviceProviderId;

        public Long getOrderId() {
            return orderId;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public Long getServiceProviderId() {
            return serviceProviderId;
        }

        public void setServiceProviderId(Long serviceProviderId) {
            this.serviceProviderId = serviceProviderId;
        }
    }
}


