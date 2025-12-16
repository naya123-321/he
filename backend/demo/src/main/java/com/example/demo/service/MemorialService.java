package com.example.demo.service;

import com.example.demo.common.PageResult;
import com.example.demo.dto.MemorialCreateDTO;
import com.example.demo.entity.Memorial;
import com.example.demo.entity.Order;
import com.example.demo.repository.MemorialRepository;
import com.example.demo.repository.OrderRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MemorialService {
    @Autowired
    private MemorialRepository memorialRepository;

    @Autowired
    private OrderRepository orderRepository;
    
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
        memorial.setDesignStatus(0); // 未提交设计
        memorial.setDesignUpdateTime(LocalDateTime.now());

        // 保存宠主上传的照片URL列表
        try {
            if (dto.getPhotoUrls() != null && !dto.getPhotoUrls().isEmpty()) {
                memorial.setPetPhotoUrls(objectMapper.writeValueAsString(dto.getPhotoUrls()));
                // 默认封面取第一张
                if (memorial.getCoverImage() == null || memorial.getCoverImage().isBlank()) {
                    memorial.setCoverImage(dto.getPhotoUrls().get(0));
                }
            }
        } catch (Exception ignored) {
        }
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
        Page<Memorial> page = memorialRepository.findByConditions(userId, status, null, petName, title, pageable);
        
        return new PageResult<>(
            page.getContent().stream().map(this::convertToVO).collect(Collectors.toList()),
            page.getTotalElements(),
            pageSize,
            pageNum
        );
    }

    /**
     * 获取纪念册列表（支持设计协作状态过滤）
     */
    public PageResult<MemorialVO> getMemorialListWithDesignStatus(Long userId, Integer status, Integer designStatus,
                                                                  String petName, String title, Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Memorial> page = memorialRepository.findByConditions(userId, status, designStatus, petName, title, pageable);

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
        // 服务端列表：包含“指派给我” + “待认领(未指派且已提交设计)”任务
        Page<Memorial> page = memorialRepository.findProviderTasks(serviceProviderId, status, null, petName, title, pageable);
        
        return new PageResult<>(
            page.getContent().stream().map(this::convertToVO).collect(Collectors.toList()),
            page.getTotalElements(),
            pageSize,
            pageNum
        );
    }

    /**
     * 服务人员获取纪念册列表（支持设计协作状态过滤）
     */
    public PageResult<MemorialVO> getMemorialListByServiceProviderWithDesignStatus(Long serviceProviderId, Integer status,
                                                                                   Integer designStatus, String petName, String title,
                                                                                   Integer pageNum, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        Page<Memorial> page = memorialRepository.findProviderTasks(serviceProviderId, status, designStatus, petName, title, pageable);

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

    /**
     * 宠主绑定/修改关联订单（用于提交设计）
     */
    @Transactional
    public MemorialVO bindOrder(Long memorialId, Long ownerId, Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("纪念册不存在"));

        if (!memorial.getUserId().equals(ownerId)) {
            throw new RuntimeException("无权操作");
        }

        // 已进入协作流程后不允许改订单，避免串单
        if (memorial.getDesignStatus() != null && memorial.getDesignStatus() >= 10) {
            throw new RuntimeException("纪念册已进入设计协作流程，无法修改关联订单");
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!order.getUserId().equals(ownerId)) {
            throw new RuntimeException("只能关联自己的订单");
        }

        memorial.setOrderId(orderId);
        // 若订单已有服务人员，提前带入，便于提交设计
        if (order.getServiceProviderId() != null) {
            memorial.setDesignProviderId(order.getServiceProviderId());
        }
        memorial.setDesignUpdateTime(LocalDateTime.now());
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    // ===== 设计协作流程 =====

    /**
     * 宠主提交到服务端设计
     */
    @Transactional
    public MemorialVO submitForDesign(Long memorialId, Long ownerId) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("纪念册不存在"));

        if (!memorial.getUserId().equals(ownerId)) {
            throw new RuntimeException("无权操作");
        }

        // 必须先有宠物照片
        if (memorial.getPetPhotoUrls() == null || memorial.getPetPhotoUrls().isBlank()) {
            throw new RuntimeException("请先上传宠物照片后再提交设计");
        }

        // 必须有关联订单（服务人员可后续自行接单/认领）
        if (memorial.getOrderId() == null) {
            throw new RuntimeException("纪念册未关联订单，无法提交设计");
        }
        Order order = orderRepository.findById(memorial.getOrderId())
            .orElseThrow(() -> new RuntimeException("关联订单不存在"));
        // 若订单已分配服务人员，直接指派；否则保留为空，待服务人员认领
        if (order.getServiceProviderId() != null) {
            memorial.setDesignProviderId(order.getServiceProviderId());
        }
        memorial.setDesignStatus(10); // 已提交服务端设计
        memorial.setDesignUpdateTime(LocalDateTime.now());
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 服务端回传设计稿（图片/或 PDF）
     */
    @Transactional
    public MemorialVO providerUploadDraft(Long memorialId, Long providerId, List<String> images, String pdfUrl, String message) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("纪念册不存在"));

        var provider = userService.getUserInfo(providerId);
        if (provider.getRole() == null || (provider.getRole() != 1 && provider.getRole() != 2)) {
            throw new RuntimeException("无权操作");
        }

        // 若未指定服务人员，允许服务人员“接单式”认领；否则要求匹配
        if (memorial.getDesignProviderId() == null && provider.getRole() == 1) {
            memorial.setDesignProviderId(providerId);
            // 同步订单服务人员（实现“服务人员可自行接单”）
            if (memorial.getOrderId() != null) {
                try {
                    Order order = orderRepository.findById(memorial.getOrderId()).orElse(null);
                    if (order != null) {
                        if (order.getServiceProviderId() == null) {
                            order.setServiceProviderId(providerId);
                            orderRepository.save(order);
                        } else if (!providerId.equals(order.getServiceProviderId())) {
                            // 订单已被分配给其他服务人员，拒绝认领
                            throw new RuntimeException("该订单已被分配给其他服务人员，无法接单");
                        }
                    }
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception ignored) {
                }
            }
        } else if (provider.getRole() == 1 && !providerId.equals(memorial.getDesignProviderId())) {
            throw new RuntimeException("无权操作：非指派服务人员");
        }

        try {
            if (images != null && !images.isEmpty()) {
                memorial.setDesignDraftImages(objectMapper.writeValueAsString(images));
            }
        } catch (Exception ignored) {}

        if (pdfUrl != null && !pdfUrl.isBlank()) {
            memorial.setDesignDraftPdfUrl(pdfUrl);
        }
        if (message != null) {
            memorial.setDesignMessage(message);
        }

        memorial.setDesignStatus(20); // 服务端已回传设计稿
        memorial.setDesignUpdateTime(LocalDateTime.now());
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 宠主回传修改稿（图片/或 PDF）
     */
    @Transactional
    public MemorialVO ownerUploadFeedback(Long memorialId, Long ownerId, List<String> images, String pdfUrl, String message) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("纪念册不存在"));

        if (!memorial.getUserId().equals(ownerId)) {
            throw new RuntimeException("无权操作");
        }

        try {
            if (images != null && !images.isEmpty()) {
                memorial.setOwnerFeedbackImages(objectMapper.writeValueAsString(images));
            }
        } catch (Exception ignored) {}

        if (pdfUrl != null && !pdfUrl.isBlank()) {
            memorial.setOwnerFeedbackPdfUrl(pdfUrl);
        }
        if (message != null) {
            memorial.setDesignMessage(message);
        }

        memorial.setDesignStatus(30); // 宠主已回传修改稿
        memorial.setDesignUpdateTime(LocalDateTime.now());
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 宠主确认最终版
     */
    @Transactional
    public MemorialVO ownerConfirmFinal(Long memorialId, Long ownerId) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("纪念册不存在"));

        if (!memorial.getUserId().equals(ownerId)) {
            throw new RuntimeException("无权操作");
        }

        if (memorial.getDesignStatus() == null || memorial.getDesignStatus() < 20) {
            throw new RuntimeException("尚未收到服务端设计稿，无法确认最终版");
        }

        memorial.setDesignStatus(40);
        memorial.setDesignUpdateTime(LocalDateTime.now());
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 服务端上传最终版并提交管理员审核
     */
    @Transactional
    public MemorialVO providerUploadFinalAndSubmitReview(Long memorialId, Long providerId, List<String> images, String pdfUrl, String message) {
        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("纪念册不存在"));

        var provider = userService.getUserInfo(providerId);
        if (provider.getRole() == null || (provider.getRole() != 1 && provider.getRole() != 2)) {
            throw new RuntimeException("无权操作");
        }
        if (provider.getRole() == 1 && (memorial.getDesignProviderId() == null || !providerId.equals(memorial.getDesignProviderId()))) {
            throw new RuntimeException("无权操作：非指派服务人员");
        }

        if (memorial.getDesignStatus() == null || memorial.getDesignStatus() < 40) {
            throw new RuntimeException("宠主尚未确认最终版，无法提交管理员审核");
        }

        try {
            if (images != null && !images.isEmpty()) {
                memorial.setDesignFinalImages(objectMapper.writeValueAsString(images));
            }
        } catch (Exception ignored) {}

        if (pdfUrl != null && !pdfUrl.isBlank()) {
            memorial.setDesignFinalPdfUrl(pdfUrl);
        }
        if (message != null) {
            memorial.setDesignMessage(message);
        }

        memorial.setDesignStatus(50); // 服务端已提交管理员审核
        memorial.setStatus(1); // 待审核
        memorial.setDesignUpdateTime(LocalDateTime.now());
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 管理员审核
     */
    @Transactional
    public MemorialVO adminReview(Long memorialId, Long adminId, boolean approved, String comment, Boolean isPublic) {
        var admin = userService.getUserInfo(adminId);
        if (admin.getRole() == null || admin.getRole() != 2) {
            throw new RuntimeException("无权操作：仅管理员可审核");
        }

        Memorial memorial = memorialRepository.findById(memorialId)
            .orElseThrow(() -> new RuntimeException("纪念册不存在"));

        if (memorial.getDesignStatus() == null || memorial.getDesignStatus() < 50) {
            throw new RuntimeException("该纪念册未提交审核");
        }

        memorial.setAdminReviewTime(LocalDateTime.now());
        memorial.setAdminReviewComment(comment);

        if (approved) {
            memorial.setDesignStatus(60);
            memorial.setStatus(2); // 已发布
            memorial.setPublishTime(LocalDateTime.now());
            memorial.setIsPublic(isPublic != null ? isPublic : true);
            if (Boolean.TRUE.equals(memorial.getIsPublic()) && (memorial.getShareToken() == null || memorial.getShareToken().isBlank())) {
                memorial.setShareToken(java.util.UUID.randomUUID().toString().replace("-", ""));
            }
        } else {
            memorial.setDesignStatus(61);
            // 保持待审核状态，允许服务端继续修改后再提交
            memorial.setStatus(1);
        }

        memorial.setDesignUpdateTime(LocalDateTime.now());
        memorialRepository.save(memorial);
        return convertToVO(memorial);
    }

    /**
     * 分享访问（公开、且管理员已通过）
     */
    public MemorialVO getSharedMemorial(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("无效的分享链接");
        }
        Memorial memorial = memorialRepository.findByShareToken(token)
            .orElseThrow(() -> new RuntimeException("分享内容不存在"));

        if (!Boolean.TRUE.equals(memorial.getIsPublic()) || memorial.getDesignStatus() == null || memorial.getDesignStatus() != 60) {
            throw new RuntimeException("该纪念册未公开或未通过审核");
        }
        return convertToVO(memorial);
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

        // ===== 设计协作字段补齐 =====
        vo.setDesignStatus(memorial.getDesignStatus());
        vo.setDesignStatusText(getDesignStatusText(memorial.getDesignStatus()));
        vo.setDesignProviderId(memorial.getDesignProviderId());
        if (memorial.getDesignProviderId() != null) {
            try {
                var provider = userService.getUserInfo(memorial.getDesignProviderId());
                vo.setDesignProviderName(provider.getUsername());
            } catch (Exception ignored) {
            }
        }

        // 解析 JSON 数组字段
        try {
            if (memorial.getPetPhotoUrls() != null && !memorial.getPetPhotoUrls().isBlank()) {
                vo.setPetPhotoUrls(objectMapper.readValue(memorial.getPetPhotoUrls(), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
            }
        } catch (Exception ignored) {}
        try {
            if (memorial.getDesignDraftImages() != null && !memorial.getDesignDraftImages().isBlank()) {
                vo.setDesignDraftImages(objectMapper.readValue(memorial.getDesignDraftImages(), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
            }
        } catch (Exception ignored) {}
        vo.setDesignDraftPdfUrl(memorial.getDesignDraftPdfUrl());
        try {
            if (memorial.getOwnerFeedbackImages() != null && !memorial.getOwnerFeedbackImages().isBlank()) {
                vo.setOwnerFeedbackImages(objectMapper.readValue(memorial.getOwnerFeedbackImages(), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
            }
        } catch (Exception ignored) {}
        vo.setOwnerFeedbackPdfUrl(memorial.getOwnerFeedbackPdfUrl());
        try {
            if (memorial.getDesignFinalImages() != null && !memorial.getDesignFinalImages().isBlank()) {
                vo.setDesignFinalImages(objectMapper.readValue(memorial.getDesignFinalImages(), objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
            }
        } catch (Exception ignored) {}
        vo.setDesignFinalPdfUrl(memorial.getDesignFinalPdfUrl());
        vo.setDesignMessage(memorial.getDesignMessage());
        vo.setDesignUpdateTime(memorial.getDesignUpdateTime());
        vo.setAdminReviewTime(memorial.getAdminReviewTime());
        vo.setAdminReviewComment(memorial.getAdminReviewComment());
        vo.setIsPublic(memorial.getIsPublic());
        vo.setShareToken(memorial.getShareToken());
        
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

    private String getDesignStatusText(Integer status) {
        if (status == null) return "未提交设计";
        switch (status) {
            case 0: return "未提交设计";
            case 10: return "已提交服务端设计";
            case 20: return "服务端已回传设计稿";
            case 30: return "宠主已回传修改稿";
            case 40: return "宠主已确认最终版";
            case 50: return "服务端已提交管理员审核";
            case 60: return "管理员已通过";
            case 61: return "管理员已拒绝";
            default: return "未知";
        }
    }
}


