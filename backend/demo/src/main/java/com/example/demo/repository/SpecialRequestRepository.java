package com.example.demo.repository;

import com.example.demo.entity.SpecialRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialRequestRepository extends JpaRepository<SpecialRequest, Long> {
    // 根据订单ID查找特殊需求
    Optional<SpecialRequest> findByOrderId(Long orderId);
    
    // 根据用户ID查找特殊需求列表
    Page<SpecialRequest> findByUserId(Long userId, Pageable pageable);
    
    // 根据状态查找特殊需求列表
    Page<SpecialRequest> findByStatus(Integer status, Pageable pageable);
    
    // 根据订单ID和状态查找
    List<SpecialRequest> findByOrderIdAndStatus(Long orderId, Integer status);
    
    // 查询所有特殊需求（管理员用）
    @Query("SELECT sr FROM SpecialRequest sr ORDER BY sr.createTime DESC")
    Page<SpecialRequest> findAllOrderByCreateTimeDesc(Pageable pageable);
}


