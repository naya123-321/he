package com.example.demo.repository;

import com.example.demo.entity.UrnStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UrnStorageRepository extends JpaRepository<UrnStorage, Long> {
    /**
     * 根据用户ID查询
     */
    Page<UrnStorage> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据状态查询
     */
    Page<UrnStorage> findByStatus(Integer status, Pageable pageable);

    /**
     * 根据订单ID查询
     */
    List<UrnStorage> findByOrderId(Long orderId);

    /**
     * 根据到期日期查询即将到期的记录
     */
    @Query("SELECT u FROM UrnStorage u WHERE u.status = 0 AND u.expiryDate BETWEEN :startDate AND :endDate")
    List<UrnStorage> findExpiringSoon(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 综合查询
     */
    @Query("SELECT u FROM UrnStorage u WHERE " +
           "(:userId IS NULL OR u.userId = :userId) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:petName IS NULL OR u.petName LIKE CONCAT('%', :petName, '%')) AND " +
           "(:urnNo IS NULL OR u.urnNo LIKE CONCAT('%', :urnNo, '%')) AND " +
           "(:orderId IS NULL OR u.orderId = :orderId)")
    Page<UrnStorage> findByConditions(
        @Param("userId") Long userId,
        @Param("status") Integer status,
        @Param("petName") String petName,
        @Param("urnNo") String urnNo,
        @Param("orderId") Long orderId,
        Pageable pageable
    );
}

