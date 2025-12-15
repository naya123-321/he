package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.SpecialRequestCreateDTO;
import com.example.demo.dto.SpecialRequestReviewDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.SpecialRequest;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.SpecialRequestRepository;
import com.example.demo.vo.SpecialRequestVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SpecialRequestService {
    @Autowired
    private SpecialRequestRepository specialRequestRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private UserService userService;

    /**
     * 创建特殊需求
     */
    @Transactional
    public SpecialRequestVO createSpecialRequest(Long userId, SpecialRequestCreateDTO dto) {
        // 验证订单是否存在且属于当前用户
        Optional<Order> orderOpt = orderRepository.findById(dto.getOrderId());
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("订单不存在");
        }
        Order order = orderOpt.get();
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该订单");
        }

        // 检查是否已有待审核的特殊需求
        List<SpecialRequest> existingRequests = specialRequestRepository.findByOrderIdAndStatus(
            dto.getOrderId(), 0
        );
        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("该订单已有待审核的特殊需求，请等待审核");
        }

        SpecialRequest request = new SpecialRequest();
        request.setOrderId(dto.getOrderId());
        request.setUserId(userId);
        request.setRequestType(dto.getRequestType() != null ? dto.getRequestType() : "其他");
        request.setDescription(dto.getDescription());
        request.setStatus(0); // 待审核

        SpecialRequest saved = specialRequestRepository.save(request);
        return convertToVO(saved);
    }

    /**
     * 获取特殊需求列表（管理员）
     */
    public PageResult<SpecialRequestVO> getSpecialRequestList(Integer status, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<SpecialRequest> page;
        
        if (status != null) {
            page = specialRequestRepository.findByStatus(status, pageable);
        } else {
            page = specialRequestRepository.findAllOrderByCreateTimeDesc(pageable);
        }
        
        List<SpecialRequestVO> vos = page.getContent().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return new PageResult<>(vos, page.getTotalElements(), pageSize, pageNum);
    }

    /**
     * 获取用户的特殊需求列表
     */
    public PageResult<SpecialRequestVO> getUserSpecialRequestList(Long userId, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<SpecialRequest> page = specialRequestRepository.findByUserId(userId, pageable);
        
        List<SpecialRequestVO> vos = page.getContent().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return new PageResult<>(vos, page.getTotalElements(), pageSize, pageNum);
    }

    /**
     * 获取特殊需求详情
     */
    public SpecialRequestVO getSpecialRequestDetail(Long id) {
        Optional<SpecialRequest> requestOpt = specialRequestRepository.findById(id);
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("特殊需求不存在");
        }
        return convertToVO(requestOpt.get());
    }

    /**
     * 根据订单ID获取已通过审核的特殊需求
     */
    public SpecialRequestVO getApprovedSpecialRequestByOrderId(Long orderId) {
        Optional<SpecialRequest> requestOpt = specialRequestRepository.findByOrderId(orderId);
        if (requestOpt.isEmpty()) {
            return null; // 没有特殊需求
        }
        SpecialRequest request = requestOpt.get();
        // 只返回已通过审核的特殊需求（status = 1）
        if (request.getStatus() == 1) {
            return convertToVO(request);
        }
        return null; // 未通过审核或待审核
    }

    /**
     * 审核特殊需求
     */
    @Transactional
    public SpecialRequestVO reviewSpecialRequest(Long adminId, SpecialRequestReviewDTO dto) {
        Optional<SpecialRequest> requestOpt = specialRequestRepository.findById(dto.getRequestId());
        if (requestOpt.isEmpty()) {
            throw new RuntimeException("特殊需求不存在");
        }
        
        SpecialRequest request = requestOpt.get();
        if (request.getStatus() != 0) {
            throw new RuntimeException("该需求已审核，无法重复审核");
        }
        
        request.setStatus(dto.getStatus()); // 1:已通过 2:已拒绝
        request.setAdminComment(dto.getAdminComment());
        request.setAdminId(adminId);
        request.setReviewTime(LocalDateTime.now());
        
        SpecialRequest saved = specialRequestRepository.save(request);
        return convertToVO(saved);
    }

    /**
     * 转换为VO
     */
    private SpecialRequestVO convertToVO(SpecialRequest request) {
        SpecialRequestVO vo = new SpecialRequestVO();
        BeanUtils.copyProperties(request, vo);
        
        // 设置订单号
        Optional<Order> orderOpt = orderRepository.findById(request.getOrderId());
        if (orderOpt.isPresent()) {
            vo.setOrderNo(orderOpt.get().getOrderNo());
        }
        
        // 设置用户信息
        try {
            var userInfo = userService.getUserInfo(request.getUserId());
            vo.setUserName(userInfo.getUsername());
            vo.setUserPhone(userInfo.getPhone());
        } catch (Exception e) {
            vo.setUserName("未知用户");
        }
        
        // 设置管理员信息
        if (request.getAdminId() != null) {
            try {
                var adminInfo = userService.getUserInfo(request.getAdminId());
                vo.setAdminName(adminInfo.getUsername());
            } catch (Exception e) {
                vo.setAdminName("未知管理员");
            }
        }
        
        // 设置状态文本
        switch (request.getStatus()) {
            case 0:
                vo.setStatusText("待审核");
                break;
            case 1:
                vo.setStatusText("已通过");
                break;
            case 2:
                vo.setStatusText("已拒绝");
                break;
            case 3:
                vo.setStatusText("处理中");
                break;
            default:
                vo.setStatusText("未知");
        }
        
        return vo;
    }
}


