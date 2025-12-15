package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.FeedbackCreateDTO;
import com.example.demo.dto.FeedbackResponseDTO;
import com.example.demo.entity.Feedback;
import com.example.demo.entity.Order;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.vo.FeedbackVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FeedbackService {
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    /**
     * 创建反馈（宠主提交）
     */
    @Transactional
    public FeedbackVO createFeedback(Long userId, FeedbackCreateDTO dto) {
        // 验证订单是否存在且属于该用户
        Optional<Order> orderOpt = orderRepository.findById(dto.getOrderId());
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getServiceProviderId() == null) {
            throw new RuntimeException("订单尚未分配服务人员，无法提交反馈");
        }

        // 允许多次提交反馈，不再限制
        Feedback feedback = new Feedback();
        feedback.setOrderId(dto.getOrderId());
        feedback.setUserId(userId);
        feedback.setServiceProviderId(order.getServiceProviderId());
        feedback.setContent(dto.getContent());
        feedback.setStatus(0); // 待处理
        feedback.setIsFromProvider(false); // 标记为宠主提交

        Feedback saved = feedbackRepository.save(feedback);
        return convertToVO(saved);
    }

    /**
     * 创建反馈（服务人员主动提交给宠主）
     */
    @Transactional
    public FeedbackVO createFeedbackByProvider(Long serviceProviderId, FeedbackCreateDTO dto) {
        // 验证订单是否存在
        Optional<Order> orderOpt = orderRepository.findById(dto.getOrderId());
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        
        // 验证订单是否分配给该服务人员
        if (order.getServiceProviderId() == null || !order.getServiceProviderId().equals(serviceProviderId)) {
            throw new RuntimeException("无权操作此订单，订单未分配给您");
        }

        // 创建反馈，服务人员主动提交给宠主
        Feedback feedback = new Feedback();
        feedback.setOrderId(dto.getOrderId());
        feedback.setUserId(order.getUserId()); // 宠主ID
        feedback.setServiceProviderId(serviceProviderId);
        feedback.setContent(dto.getContent());
        feedback.setStatus(0); // 待处理（宠主可以查看）
        feedback.setIsFromProvider(true); // 标记为服务人员主动发送

        Feedback saved = feedbackRepository.save(feedback);
        return convertToVO(saved);
    }

    /**
     * 获取服务人员的反馈列表（只返回宠主提交的反馈，排除服务人员主动发送的）
     */
    public PageResult<FeedbackVO> getFeedbackListByProvider(Long serviceProviderId, Integer status, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Feedback> page;

        if (status != null) {
            // 只查询宠主提交的反馈（isFromProvider = false），排除服务人员主动发送的
            page = feedbackRepository.findByServiceProviderIdAndStatusAndIsFromProviderFalse(serviceProviderId, status, pageable);
        } else {
            // 只查询宠主提交的反馈（isFromProvider = false），排除服务人员主动发送的
            page = feedbackRepository.findByServiceProviderIdAndIsFromProviderFalse(serviceProviderId, pageable);
        }

        List<FeedbackVO> feedbackVOs = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(feedbackVOs, page.getTotalElements(), pageSize, pageNum);
    }

    /**
     * 获取用户的反馈列表
     */
    public PageResult<FeedbackVO> getFeedbackListByUser(Long userId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Feedback> page = feedbackRepository.findByUserId(userId, pageable);

        List<FeedbackVO> feedbackVOs = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(feedbackVOs, page.getTotalElements(), pageSize, pageNum);
    }

    /**
     * 处理反馈（服务人员回复）
     */
    @Transactional
    public FeedbackVO respondToFeedback(Long serviceProviderId, FeedbackResponseDTO dto) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(dto.getFeedbackId());
        if (feedbackOpt.isEmpty()) {
            throw new RuntimeException("反馈不存在");
        }
        Feedback feedback = feedbackOpt.get();

        // 验证是否是分配给该服务人员的反馈
        if (!feedback.getServiceProviderId().equals(serviceProviderId)) {
            throw new RuntimeException("无权处理此反馈");
        }

        feedback.setResponse(dto.getResponse());
        feedback.setStatus(1); // 已处理
        feedback.setResponseTime(LocalDateTime.now());

        Feedback saved = feedbackRepository.save(feedback);
        return convertToVO(saved);
    }

    /**
     * 宠主回复服务人员主动发送的反馈
     */
    @Transactional
    public FeedbackVO respondToProviderFeedback(Long userId, FeedbackResponseDTO dto) {
        Optional<Feedback> feedbackOpt = feedbackRepository.findById(dto.getFeedbackId());
        if (feedbackOpt.isEmpty()) {
            throw new RuntimeException("反馈不存在");
        }
        Feedback feedback = feedbackOpt.get();

        // 验证是否是发送给该宠主的反馈，且是服务人员主动发送的
        if (!feedback.getUserId().equals(userId)) {
            throw new RuntimeException("无权回复此反馈");
        }
        if (!feedback.getIsFromProvider()) {
            throw new RuntimeException("此反馈不是服务人员主动发送的，无法回复");
        }

        feedback.setResponse(dto.getResponse());
        feedback.setStatus(1); // 已处理（宠主已回复）
        feedback.setResponseTime(LocalDateTime.now());

        Feedback saved = feedbackRepository.save(feedback);
        return convertToVO(saved);
    }

    /**
     * 获取服务人员主动发送的反馈列表
     */
    public PageResult<FeedbackVO> getProviderSentFeedbackList(Long serviceProviderId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Feedback> page = feedbackRepository.findByServiceProviderIdAndIsFromProviderTrue(serviceProviderId, pageable);

        List<FeedbackVO> feedbackVOs = page.getContent().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(feedbackVOs, page.getTotalElements(), pageSize, pageNum);
    }

    /**
     * 将Feedback实体转换为FeedbackVO
     */
    private FeedbackVO convertToVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        BeanUtils.copyProperties(feedback, vo);

        // 设置状态文本
        if (feedback.getStatus() != null) {
            vo.setStatusText(feedback.getStatus() == 0 ? "待处理" : "已处理");
        }

        // 获取订单信息
        Optional<Order> orderOpt = orderRepository.findById(feedback.getOrderId());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            vo.setOrderNo(order.getOrderNo());
            vo.setPetName(order.getPetName());
        }

        // 获取用户信息
        try {
            var userInfo = userService.getUserInfo(feedback.getUserId());
            vo.setUsername(userInfo.getUsername());
        } catch (Exception e) {
            vo.setUsername("未知用户");
        }

        // 获取服务人员信息
        try {
            var providerInfo = userService.getUserInfo(feedback.getServiceProviderId());
            vo.setServiceProviderName(providerInfo.getUsername());
        } catch (Exception e) {
            vo.setServiceProviderName("未知服务人员");
        }

        return vo;
    }
}

