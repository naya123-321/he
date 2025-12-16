package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.OrderCreateDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.ServiceType;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ServiceTypeRepository;
import com.example.demo.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    @Autowired
    private UserService userService;

    /**
     * 创建订单
     */
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        Order order = new Order();
        BeanUtils.copyProperties(dto, order);
        order.setUserId(userId);
        order.setStatus(0); // 待接单
        order.setPaymentStatus(0); // 未支付

        // 生成订单号
        String orderNo = "ORD" + System.currentTimeMillis();
        order.setOrderNo(orderNo);

        // 获取服务类型并设置价格
        Optional<ServiceType> serviceTypeOpt = serviceTypeRepository.findById(dto.getServiceTypeId());
        if (serviceTypeOpt.isEmpty()) {
            throw new RuntimeException("服务类型不存在");
        }
        ServiceType serviceType = serviceTypeOpt.get();
        Double price = serviceType.getPrice();
        if (price == null || price <= 0) {
            throw new RuntimeException("服务类型价格无效");
        }
        
        // 设置订单金额
        order.setTotalAmount(price);
        order.setActualAmount(price); // 实际金额等于总金额（无折扣）

        Order saved = orderRepository.save(order);
        return convertToVO(saved);
    }

    /**
     * 获取订单详情
     */
    public OrderVO getOrderDetail(Long id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        return convertToVO(orderOpt.get());
    }

    /**
     * 获取订单列表
     */
    public PageResult<OrderVO> getOrderList(Long userId, Long serviceProviderId, Integer status, 
                                           String petName, String orderNo, LocalDateTime startDate, 
                                           LocalDateTime endDate, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Order> page;
        
        // 如果userId为null（服务人员查询所有订单），且没有其他筛选条件，直接查询所有订单
        if (userId == null && serviceProviderId == null && status == null && 
            (petName == null || petName.isEmpty()) && (orderNo == null || orderNo.isEmpty()) &&
            startDate == null && endDate == null) {
            // 直接查询所有订单，按创建时间倒序
            Pageable sortedPageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
            page = orderRepository.findAll(sortedPageable);
        } else {
            // 使用条件查询
            page = orderRepository.findByConditions(
                userId, serviceProviderId, status, petName, orderNo, startDate, endDate, pageable
            );
        }
        
        List<OrderVO> orderVOs = page.getContent().stream().map(this::convertToVO).collect(Collectors.toList());
        
        return new PageResult<>(
            orderVOs,
            page.getTotalElements(),
            pageSize,
            pageNum
        );
    }

    /**
     * 检查订单是否已有服务人员，如果没有则返回要分配的服务人员ID
     */
    public Long checkAndAssignProvider(Long orderId, Long serviceProviderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return null;
        }
        Order order = orderOpt.get();
        // 如果订单还没有分配服务人员，返回要分配的服务人员ID
        if (order.getServiceProviderId() == null) {
            return serviceProviderId;
        }
        return null;
    }

    /**
     * 取消订单
     */
    @Transactional
    public boolean cancelOrder(Long id, String reason) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        if (order.getStatus() == 4) {
            throw new RuntimeException("订单已取消");
        }
        if (order.getStatus() == 3) {
            throw new RuntimeException("订单已完成，无法取消");
        }
        order.setStatus(4); // 已取消
        orderRepository.save(order);
        return true;
    }

    /**
     * 更新订单状态
     */
    @Transactional
    public boolean updateOrderStatus(Long id, Integer targetStatus, String remark, Long serviceProviderId) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        
        // 如果提供了服务人员ID，分配服务人员
        if (serviceProviderId != null) {
            order.setServiceProviderId(serviceProviderId);
            // 如果订单状态是待确认，自动更新为已确认
            if (order.getStatus() == 0 && targetStatus == null) {
                order.setStatus(1);
            }
        }
        
        // 如果提供了目标状态，更新状态
        if (targetStatus != null) {
            // 如果目标状态是"已确认"(1)或"服务中"(2)，且订单还没有分配服务人员，自动分配
            if ((targetStatus == 1 || targetStatus == 2) && order.getServiceProviderId() == null && serviceProviderId == null) {
                // 这里需要从上下文获取当前服务人员ID，但由于方法签名限制，暂时不处理
                // 实际调用时应该在Controller层处理
            }
            order.setStatus(targetStatus);
        }
        
        // 如果提供了备注，添加到服务记录
        if (remark != null && !remark.isEmpty()) {
            String existingNotes = order.getServiceNotes();
            if (existingNotes == null || existingNotes.isEmpty()) {
                order.setServiceNotes(remark);
            } else {
                order.setServiceNotes(existingNotes + "\n" + remark);
            }
        }
        
        // 如果状态更新为已完成，设置完成时间
        if (targetStatus != null && targetStatus == 3) {
            order.setCompletionTime(LocalDateTime.now());
        }
        
        orderRepository.save(order);
        return true;
    }

    /**
     * 分配服务人员
     */
    @Transactional
    public boolean assignServiceProvider(Long orderId, Long serviceProviderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        
        // 验证服务人员是否存在
        try {
            var providerInfo = userService.getUserInfo(serviceProviderId);
            if (providerInfo.getRole() == null || providerInfo.getRole() != 1) {
                throw new RuntimeException("指定的用户不是服务人员");
            }
        } catch (Exception e) {
            throw new RuntimeException("服务人员不存在: " + e.getMessage());
        }
        
        order.setServiceProviderId(serviceProviderId);
        // 如果订单状态是待确认，自动更新为已确认
        if (order.getStatus() == 0) {
            order.setStatus(1);
        }
        orderRepository.save(order);
        return true;
    }

    /**
     * 完成订单并评价
     */
    @Transactional
    public boolean completeOrder(Long id, Integer rating, String review) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        order.setStatus(3); // 已完成
        order.setCompletionTime(LocalDateTime.now());
        if (rating != null) {
            order.setRating(rating);
        }
        if (review != null && !review.isEmpty()) {
            order.setReview(review);
        }
        orderRepository.save(order);
        return true;
    }

    /**
     * 提交订单评价（不改变订单状态）
     * 仅允许在订单已完成(status=3)后提交
     */
    @Transactional
    public boolean submitReview(Long orderId, Long userId, Integer rating, String review) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权评价该订单");
        }
        if (order.getStatus() == null || order.getStatus() != 3) {
            throw new RuntimeException("订单未完成，无法评价");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("评分需为 1-5 星");
        }
        order.setRating(rating);
        if (review != null) {
            order.setReview(review);
        }
        orderRepository.save(order);
        return true;
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        
        // 设置状态文本
        if (order.getStatus() != null) {
            vo.setStatusText(getStatusText(order.getStatus()));
        }
        
        // 设置支付状态文本
        if (order.getPaymentStatus() != null) {
            vo.setPaymentStatusText(getPaymentStatusText(order.getPaymentStatus()));
        }
        
        // 获取服务类型信息
        try {
            Optional<ServiceType> serviceTypeOpt = serviceTypeRepository.findById(order.getServiceTypeId());
            if (serviceTypeOpt.isPresent()) {
                ServiceType serviceType = serviceTypeOpt.get();
                vo.setServiceTypeName(serviceType.getName());
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        // 获取用户信息
        try {
            var userInfo = userService.getUserInfo(order.getUserId());
            vo.setUsername(userInfo.getUsername());
            vo.setUserPhone(userInfo.getPhone());
        } catch (Exception e) {
            vo.setUsername("未知用户");
        }
        
        // 获取服务人员信息
        if (order.getServiceProviderId() != null) {
            try {
                var providerInfo = userService.getUserInfo(order.getServiceProviderId());
                vo.setServiceProviderName(providerInfo.getUsername());
                vo.setServiceProviderPhone(providerInfo.getPhone());
            } catch (Exception e) {
                vo.setServiceProviderName("未知");
            }
        }
        
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待接单";
            case 1: return "已接单";
            case 2: return "服务中";
            case 3: return "已完成";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    private String getPaymentStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "未支付";
            case 1: return "已支付";
            case 2: return "已退款";
            default: return "未知";
        }
    }

    /**
     * 获取满意度统计数据
     */
    public Map<String, Object> getSatisfactionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 查询所有有评价的订单
        List<Order> ordersWithRating = orderRepository.findOrdersWithRating();
        
        if (ordersWithRating.isEmpty()) {
            stats.put("averageSatisfaction", 0.0);
            stats.put("totalReviews", 0);
            stats.put("fiveStarReviews", 0);
            stats.put("fourStarReviews", 0);
            stats.put("threeStarReviews", 0);
            stats.put("twoStarReviews", 0);
            stats.put("oneStarReviews", 0);
            return stats;
        }
        
        // 计算总评价数
        int totalReviews = ordersWithRating.size();
        
        // 计算各星级评价数
        long fiveStar = orderRepository.countByRating(5);
        long fourStar = orderRepository.countByRating(4);
        long threeStar = orderRepository.countByRating(3);
        long twoStar = orderRepository.countByRating(2);
        long oneStar = orderRepository.countByRating(1);
        
        // 计算平均满意度
        double totalRating = ordersWithRating.stream()
            .filter(o -> o.getRating() != null)
            .mapToInt(Order::getRating)
            .sum();
        double averageSatisfaction = totalRating / totalReviews;
        
        stats.put("averageSatisfaction", Math.round(averageSatisfaction * 10.0) / 10.0); // 保留一位小数
        stats.put("totalReviews", totalReviews);
        stats.put("fiveStarReviews", fiveStar);
        stats.put("fourStarReviews", fourStar);
        stats.put("threeStarReviews", threeStar);
        stats.put("twoStarReviews", twoStar);
        stats.put("oneStarReviews", oneStar);
        
        return stats;
    }

    /**
     * 获取满意度趋势数据（按日期统计）
     */
    public Map<String, Object> getSatisfactionTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();
        
        // 默认查询最近30天
        if (days == null || days <= 0) {
            days = 30;
        }
        
        // 计算起始日期
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        // 查询指定时间范围内的有评价的订单
        List<Order> ordersWithRating = orderRepository.findOrdersWithRatingByDateRange(startDate, endDate);
        
        // 按日期分组统计
        Map<String, List<Order>> ordersByDate = ordersWithRating.stream()
            .collect(Collectors.groupingBy(order -> {
                LocalDateTime date = order.getCompletionTime() != null ? order.getCompletionTime() : order.getUpdateTime();
                if (date == null) {
                    date = order.getCreateTime();
                }
                return date.toLocalDate().toString();
            }));
        
        // 生成日期序列（最近N天）
        List<String> dateLabels = new ArrayList<>();
        List<Double> averageRatings = new ArrayList<>();
        List<Integer> reviewCounts = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.toString();
            dateLabels.add(dateStr);
            
            List<Order> dayOrders = ordersByDate.getOrDefault(dateStr, new ArrayList<>());
            if (dayOrders.isEmpty()) {
                averageRatings.add(0.0);
                reviewCounts.add(0);
            } else {
                double avg = dayOrders.stream()
                    .filter(o -> o.getRating() != null)
                    .mapToInt(Order::getRating)
                    .average()
                    .orElse(0.0);
                averageRatings.add(Math.round(avg * 10.0) / 10.0);
                reviewCounts.add(dayOrders.size());
            }
        }
        
        result.put("dateLabels", dateLabels);
        result.put("averageRatings", averageRatings);
        result.put("reviewCounts", reviewCounts);
        
        return result;
    }

    /**
     * 获取服务类型分布数据
     */
    public Map<String, Object> getServiceTypeDistribution() {
        Map<String, Object> result = new HashMap<>();
        
        // 查询每个服务类型的订单数量（已排除已取消的订单，status = 4）
        List<Object[]> distribution = orderRepository.countOrdersByServiceType();
        
        // 获取所有服务类型
        List<ServiceType> allServiceTypes = serviceTypeRepository.findAll();
        Map<Long, String> serviceTypeMap = allServiceTypes.stream()
            .collect(Collectors.toMap(ServiceType::getId, ServiceType::getName));
        
        // 构建分布数据
        List<Map<String, Object>> distributionList = new ArrayList<>();
        Map<Long, Long> countMap = distribution.stream()
            .collect(Collectors.toMap(
                arr -> ((Number) arr[0]).longValue(),
                arr -> ((Number) arr[1]).longValue()
            ));
        
        // 只为有订单的服务类型创建数据项（排除count为0的）
        for (ServiceType serviceType : allServiceTypes) {
            Long count = countMap.getOrDefault(serviceType.getId(), 0L);
            // 只添加有订单的服务类型
            if (count > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("serviceTypeId", serviceType.getId());
                item.put("serviceTypeName", serviceType.getName());
                item.put("count", count);
                distributionList.add(item);
            }
        }
        
        // 如果没有订单，返回空列表
        if (distributionList.isEmpty()) {
            result.put("distribution", new ArrayList<>());
            result.put("totalOrders", 0);
            return result;
        }
        
        // 按订单数量降序排序
        distributionList.sort((a, b) -> {
            Long countA = ((Number) a.get("count")).longValue();
            Long countB = ((Number) b.get("count")).longValue();
            return countB.compareTo(countA);
        });
        
        // 计算总订单数
        long totalOrders = distributionList.stream()
            .mapToLong(item -> ((Number) item.get("count")).longValue())
            .sum();
        
        result.put("distribution", distributionList);
        result.put("totalOrders", totalOrders);
        
        return result;
    }

    /**
     * 获取订单趋势数据（按日期统计订单数量）
     */
    public Map<String, Object> getOrderTrend(Integer days) {
        Map<String, Object> result = new HashMap<>();
        
        // 默认查询最近7天
        if (days == null || days <= 0) {
            days = 7;
        }
        
        // 计算起始日期
        LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        LocalDateTime startDate = endDate.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0);
        
        // 查询指定时间范围内的订单
        List<Order> orders = orderRepository.findOrdersByDateRange(startDate, endDate);
        
        // 按日期分组统计
        Map<String, Long> ordersByDate = orders.stream()
            .collect(Collectors.groupingBy(
                order -> order.getCreateTime().toLocalDate().toString(),
                Collectors.counting()
            ));
        
        // 生成日期序列（最近N天）
        List<String> dateLabels = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            String dateStr = date.toLocalDate().toString();
            dateLabels.add(dateStr);
            orderCounts.add(ordersByDate.getOrDefault(dateStr, 0L));
        }
        
        result.put("dateLabels", dateLabels);
        result.put("orderCounts", orderCounts);
        result.put("totalOrders", orders.size());
        
        return result;
    }
}
