package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByUsernameAndEmail(String username, String email);
    
    // 按角色查询
    Page<User> findByRole(Integer role, Pageable pageable);
    
    // 按关键词搜索（用户名、昵称、邮箱）
    @Query("SELECT u FROM User u WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "u.username LIKE CONCAT('%', :keyword, '%') OR " +
           "u.nickname LIKE CONCAT('%', :keyword, '%') OR " +
           "u.email LIKE CONCAT('%', :keyword, '%'))")
    Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 按角色和关键词查询
    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "u.username LIKE CONCAT('%', :keyword, '%') OR " +
           "u.nickname LIKE CONCAT('%', :keyword, '%') OR " +
           "u.email LIKE CONCAT('%', :keyword, '%'))")
    Page<User> findByRoleAndKeyword(@Param("role") Integer role, @Param("keyword") String keyword, Pageable pageable);
}


