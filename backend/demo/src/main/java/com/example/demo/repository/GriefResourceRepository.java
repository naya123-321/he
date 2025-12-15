package com.example.demo.repository;

import com.example.demo.entity.GriefResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GriefResourceRepository extends JpaRepository<GriefResource, Long> {
    List<GriefResource> findByStatus(Integer status);

    List<GriefResource> findByStatusAndType(Integer status, String type);

    @Query("SELECT g FROM GriefResource g WHERE " +
            "(:type IS NULL OR :type = '' OR g.type = :type) AND " +
            "(:status IS NULL OR g.status = :status)")
    Page<GriefResource> findByConditions(
            @Param("type") String type,
            @Param("status") Integer status,
            Pageable pageable
    );
}



