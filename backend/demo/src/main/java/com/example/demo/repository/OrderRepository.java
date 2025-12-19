package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNo(String orderNo);
    
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    Page<Order> findByServiceProviderId(Long serviceProviderId, Pageable pageable);
    
    Page<Order> findByStatus(Integer status, Pageable pageable);
    
    // 使用原生SQL查询，更可靠地处理NULL值
    // 注意：在原生SQL中，参数为null时需要使用特殊处理
    @Query(value = "SELECT * FROM orders WHERE " +
           "(:userId IS NULL OR user_id = :userId) AND " +
           "(:serviceProviderId IS NULL OR service_provider_id = :serviceProviderId) AND " +
           "(:status IS NULL OR status = :status) AND " +
           "(:petName IS NULL OR :petName = '' OR pet_name LIKE CONCAT('%', :petName, '%')) AND " +
           "(:orderNo IS NULL OR :orderNo = '' OR order_no LIKE CONCAT('%', :orderNo, '%')) AND " +
           "(:startDate IS NULL OR create_time >= :startDate) AND " +
           "(:endDate IS NULL OR create_time <= :endDate) " +
           "ORDER BY create_time DESC",
           nativeQuery = true,
           countQuery = "SELECT COUNT(*) FROM orders WHERE " +
           "(:userId IS NULL OR user_id = :userId) AND " +
           "(:serviceProviderId IS NULL OR service_provider_id = :serviceProviderId) AND " +
           "(:status IS NULL OR status = :status) AND " +
           "(:petName IS NULL OR :petName = '' OR pet_name LIKE CONCAT('%', :petName, '%')) AND " +
           "(:orderNo IS NULL OR :orderNo = '' OR order_no LIKE CONCAT('%', :orderNo, '%')) AND " +
           "(:startDate IS NULL OR create_time >= :startDate) AND " +
           "(:endDate IS NULL OR create_time <= :endDate)")
    Page<Order> findByConditions(
        @Param("userId") Long userId,
        @Param("serviceProviderId") Long serviceProviderId,
        @Param("status") Integer status,
        @Param("petName") String petName,
        @Param("orderNo") String orderNo,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // 查询有评价的订单
    @Query("SELECT o FROM Order o WHERE o.rating IS NOT NULL")
    List<Order> findOrdersWithRating();
    
    // 查询指定星级的评价数量
    @Query("SELECT COUNT(o) FROM Order o WHERE o.rating = :rating")
    Long countByRating(@Param("rating") Integer rating);
    
    // 查询指定时间范围内有评价的订单
    @Query("SELECT o FROM Order o WHERE o.rating IS NOT NULL AND " +
           "((o.completionTime IS NOT NULL AND o.completionTime >= :startDate AND o.completionTime <= :endDate) OR " +
           "(o.completionTime IS NULL AND o.updateTime IS NOT NULL AND o.updateTime >= :startDate AND o.updateTime <= :endDate) OR " +
           "(o.completionTime IS NULL AND o.updateTime IS NULL AND o.createTime >= :startDate AND o.createTime <= :endDate))")
    List<Order> findOrdersWithRatingByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // 统计每个服务类型的订单数量（排除已取消的订单，status = 4）
    // 使用 IN 明确指定有效的订单状态：0待确认, 1已确认, 2服务中, 3已完成
    @Query("SELECT o.serviceTypeId, COUNT(o) FROM Order o WHERE o.status IN (0, 1, 2, 3) GROUP BY o.serviceTypeId")
    List<Object[]> countOrdersByServiceType();
    
    // 统计每个宠物类型的订单数量（排除已取消的订单，status = 4，且petType不为空）
    // 使用 IN 明确指定有效的订单状态：0待确认, 1已确认, 2服务中, 3已完成
    @Query("SELECT o.petType, COUNT(o) FROM Order o WHERE o.status IN (0, 1, 2, 3) AND o.petType IS NOT NULL AND o.petType != '' GROUP BY o.petType")
    List<Object[]> countOrdersByPetType();
    
    // 查询指定时间范围内的订单（用于趋势统计）
    @Query("SELECT o FROM Order o WHERE o.createTime >= :startDate AND o.createTime <= :endDate ORDER BY o.createTime ASC")
    List<Order> findOrdersByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}


