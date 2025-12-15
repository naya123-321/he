package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.MemorialCreateDTO;
import com.example.demo.entity.Memorial;
import com.example.demo.repository.MemorialRepository;
import com.example.demo.vo.MemorialVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MemorialService {
    @Autowired
    private MemorialRepository memorialRepository;
    
    @Autowired
    private UserService userService;
    
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建纪念册
     */
    @Transactional
    public MemorialVO createMemorial(Long userId, MemorialCreateDTO dto) {
        Memorial memorial = new Memorial();
        memorial.setTitle(dto.getTitle());
        memorial.setSubtitle(dto.getSubtitle());
        memorial.setTemplateId(dto.getTemplateId());
        memorial.setOrderId(dto.getOrderId());
        memorial.setPetName(dto.getPetName());
        memorial.setPetType(dto.getPetType());
        memorial.setPetMemory(dto.getPetMemory());
        
        // 处理日期字段：如果日期不为null，直接设置；如果为null，设置为null
        memorial.setPetBirthDate(dto.getPetBirthDateAsDateTime());
        memorial.setPetDeathDate(dto.getPetDeathDateAsDateTime());
        
        memorial.setUserId(userId);
        memorial.setStatus(0); // 草稿
        memorial = memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 保存纪念册内容
     */
    @Transactional
    public boolean saveContent(Long memorialId, Object contentData, Object styleConfig) {
        Optional<Memorial> memorialOpt = memorialRepository.findById(memorialId);
        if (memorialOpt.isEmpty()) {
            throw new RuntimeException("纪念册不存在");
        }
        Memorial memorial = memorialOpt.get();
        try {
            if (contentData != null) {
                memorial.setContentData(objectMapper.writeValueAsString(contentData));
            }
            if (styleConfig != null) {
                memorial.setStyleConfig(objectMapper.writeValueAsString(styleConfig));
            }
            memorialRepository.save(memorial);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("保存失败: " + e.getMessage());
        }
    }

    /**
     * 发布纪念册
     */
    @Transactional
    public MemorialVO publishMemorial(Long memorialId, Boolean isPublic) {
        Optional<Memorial> memorialOpt = memorialRepository.findById(memorialId);
        if (memorialOpt.isEmpty()) {
            throw new RuntimeException("纪念册不存在");
        }
        Memorial memorial = memorialOpt.get();
        memorial.setStatus(isPublic != null && isPublic ? 2 : 1); // 2:已发布 1:待审核
        memorial.setPublishTime(LocalDateTime.now());
        memorial = memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 获取纪念册列表
     */
    public PageResult<MemorialVO> getMemorialList(Long userId, Integer status, String petName, 
                                                   String title, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Memorial> page = memorialRepository.findByConditions(userId, status, petName, title, pageable);
        
        return new PageResult<>(
            page.getContent().stream().map(this::convertToVO).collect(Collectors.toList()),
            page.getTotalElements(),
            pageSize,
            pageNum
        );
    }

    /**
     * 根据服务人员ID获取相关纪念册列表（通过订单关联）
     */
    public PageResult<MemorialVO> getMemorialListByServiceProvider(Long serviceProviderId, Integer status, 
                                                                    String petName, String title, 
                                                                    Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Memorial> page = memorialRepository.findByServiceProviderId(serviceProviderId, status, petName, title, pageable);
        
        return new PageResult<>(
            page.getContent().stream().map(this::convertToVO).collect(Collectors.toList()),
            page.getTotalElements(),
            pageSize,
            pageNum
        );
    }

    /**
     * 获取纪念册详情
     */
    public MemorialVO getMemorialDetail(Long id) {
        Optional<Memorial> memorialOpt = memorialRepository.findById(id);
        if (memorialOpt.isEmpty()) {
            throw new RuntimeException("纪念册不存在");
        }
        Memorial memorial = memorialOpt.get();
        // 增加浏览量
        memorial.setViewCount(memorial.getViewCount() + 1);
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 根据订单ID获取纪念册（一个订单可能关联多个纪念册，返回第一个）
     */
    public MemorialVO getMemorialByOrderId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        var memorials = memorialRepository.findByOrderId(orderId);
        if (memorials == null || memorials.isEmpty()) {
            return null;
        }
        // 返回第一个纪念册
        return convertToVO(memorials.get(0));
    }

    /**
     * 点赞
     */
    @Transactional
    public boolean likeMemorial(Long id) {
        Optional<Memorial> memorialOpt = memorialRepository.findById(id);
        if (memorialOpt.isEmpty()) {
            throw new RuntimeException("纪念册不存在");
        }
        Memorial memorial = memorialOpt.get();
        memorial.setLikeCount(memorial.getLikeCount() + 1);
        memorialRepository.save(memorial);
        return true;
    }

    /**
     * 取消点赞
     */
    @Transactional
    public boolean unlikeMemorial(Long id) {
        Optional<Memorial> memorialOpt = memorialRepository.findById(id);
        if (memorialOpt.isEmpty()) {
            throw new RuntimeException("纪念册不存在");
        }
        Memorial memorial = memorialOpt.get();
        if (memorial.getLikeCount() > 0) {
            memorial.setLikeCount(memorial.getLikeCount() - 1);
        }
        memorialRepository.save(memorial);
        return true;
    }

    /**
     * 删除纪念册
     */
    @Transactional
    public boolean deleteMemorial(Long id, Long userId) {
        Optional<Memorial> memorialOpt = memorialRepository.findById(id);
        if (memorialOpt.isEmpty()) {
            throw new RuntimeException("纪念册不存在");
        }
        Memorial memorial = memorialOpt.get();
        // 检查权限：只有纪念册的所有者可以删除
        if (!memorial.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此纪念册");
        }
        memorialRepository.delete(memorial);
        return true;
    }

    private MemorialVO convertToVO(Memorial memorial) {
        MemorialVO vo = new MemorialVO();
        BeanUtils.copyProperties(memorial, vo);
        
        // 设置用户信息
        try {
            var userVO = userService.getUserInfo(memorial.getUserId());
            vo.setUsername(userVO.getUsername());
            vo.setAvatar(userVO.getAvatar());
        } catch (Exception e) {
            // 忽略
        }
        
        // 设置状态文本
        vo.setStatusText(getStatusText(memorial.getStatus()));
        
        // 解析内容数据
        try {
            if (memorial.getContentData() != null) {
                vo.setPreviewContent(objectMapper.readValue(memorial.getContentData(), Object.class));
            }
        } catch (Exception e) {
            // 忽略
        }
        
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "待审核";
            case 2: return "已发布";
            case 3: return "已下架";
            default: return "未知";
        }
    }
}


