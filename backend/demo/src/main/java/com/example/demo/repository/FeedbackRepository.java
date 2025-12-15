package com.example.demo.repository;

import com.example.demo.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // 根据服务人员ID查询反馈（只查询宠主提交的反馈，排除服务人员主动发送的）
    Page<Feedback> findByServiceProviderIdAndIsFromProviderFalse(Long serviceProviderId, Pageable pageable);
    
    // 根据服务人员ID和状态查询反馈（只查询宠主提交的反馈，排除服务人员主动发送的）
    Page<Feedback> findByServiceProviderIdAndStatusAndIsFromProviderFalse(Long serviceProviderId, Integer status, Pageable pageable);
    
    // 根据服务人员ID查询服务人员主动发送的反馈
    Page<Feedback> findByServiceProviderIdAndIsFromProviderTrue(Long serviceProviderId, Pageable pageable);
    
    // 根据用户ID查询反馈（包括服务人员主动发送给该用户的反馈）
    Page<Feedback> findByUserId(Long userId, Pageable pageable);
    
    // 根据订单ID查询反馈
    List<Feedback> findByOrderId(Long orderId);
    
    // 根据订单ID查询单个反馈
    Optional<Feedback> findByOrderIdAndUserId(Long orderId, Long userId);
}

