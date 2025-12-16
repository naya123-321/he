package com.example.demo.repository;

import com.example.demo.entity.Memorial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemorialRepository extends JpaRepository<Memorial, Long> {
    Page<Memorial> findByUserId(Long userId, Pageable pageable);
    
    Page<Memorial> findByStatus(Integer status, Pageable pageable);
    
    @Query("SELECT m FROM Memorial m WHERE " +
           "(:userId IS NULL OR m.userId = :userId) AND " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:designStatus IS NULL OR m.designStatus = :designStatus) AND " +
           "(:petName IS NULL OR m.petName LIKE %:petName%) AND " +
           "(:title IS NULL OR m.title LIKE %:title%)")
    Page<Memorial> findByConditions(
        @Param("userId") Long userId,
        @Param("status") Integer status,
        @Param("designStatus") Integer designStatus,
        @Param("petName") String petName,
        @Param("title") String title,
        Pageable pageable
    );
    
    List<Memorial> findByOrderId(Long orderId);
    
    /**
     * 根据服务人员ID获取相关纪念册（通过订单关联）
     */
    @Query("SELECT DISTINCT m FROM Memorial m " +
           "INNER JOIN Order o ON m.orderId = o.id " +
           "WHERE o.serviceProviderId = :serviceProviderId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:designStatus IS NULL OR m.designStatus = :designStatus) " +
           "AND (:petName IS NULL OR m.petName LIKE CONCAT('%', :petName, '%')) " +
           "AND (:title IS NULL OR m.title LIKE CONCAT('%', :title, '%'))")
    Page<Memorial> findByServiceProviderId(
        @Param("serviceProviderId") Long serviceProviderId,
        @Param("status") Integer status,
        @Param("designStatus") Integer designStatus,
        @Param("petName") String petName,
        @Param("title") String title,
        Pageable pageable
    );

    /**
     * 服务人员任务列表：
     * - 指派给当前服务人员（m.designProviderId = :serviceProviderId）
     * - 或者待认领的设计任务（m.designProviderId IS NULL 且 m.designStatus = 10 且 m.orderId IS NOT NULL）
     */
    @Query("SELECT m FROM Memorial m WHERE " +
           "((m.designProviderId = :serviceProviderId) OR " +
           "(m.designProviderId IS NULL AND m.designStatus = 10 AND m.orderId IS NOT NULL)) AND " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:designStatus IS NULL OR m.designStatus = :designStatus) AND " +
           "(:petName IS NULL OR m.petName LIKE CONCAT('%', :petName, '%')) AND " +
           "(:title IS NULL OR m.title LIKE CONCAT('%', :title, '%'))")
    Page<Memorial> findProviderTasks(
            @Param("serviceProviderId") Long serviceProviderId,
            @Param("status") Integer status,
            @Param("designStatus") Integer designStatus,
            @Param("petName") String petName,
            @Param("title") String title,
            Pageable pageable
    );

    Optional<Memorial> findByShareToken(String shareToken);
}


