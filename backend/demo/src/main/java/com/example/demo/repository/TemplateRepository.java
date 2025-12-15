package com.example.demo.repository;

import com.example.demo.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    // 根据状态查询
    List<Template> findByStatus(Integer status);
    
    // 根据分类查询
    List<Template> findByCategory(String category);
    
    // 根据状态和分类查询
    List<Template> findByStatusAndCategory(Integer status, String category);
    
    // 根据创建者查询
    List<Template> findByCreatedBy(Long createdBy);
    
    // 搜索模板（名称或描述）
    @Query("SELECT t FROM Template t WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "t.name LIKE %:keyword% OR t.description LIKE %:keyword%) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:category IS NULL OR :category = '' OR t.category = :category)")
    List<Template> searchTemplates(
        @Param("keyword") String keyword,
        @Param("status") Integer status,
        @Param("category") String category
    );
}


