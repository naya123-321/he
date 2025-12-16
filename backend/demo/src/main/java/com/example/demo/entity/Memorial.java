package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 纪念册实体类
 */
@Entity
@Table(name = "memorials")
public class Memorial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(length = 255)
    private String coverImage;

    @Column(nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 50)
    private String petName;

    @Column(length = 50)
    private String petType;

    private LocalDateTime petBirthDate;

    private LocalDateTime petDeathDate;

    @Column(length = 2000)
    private String petMemory;

    @Column(nullable = false)
    private Integer status = 0; // 0:草稿 1:待审核 2:已发布 3:已下架

    @Column(columnDefinition = "TEXT")
    private String contentData; // JSON格式的内容数据

    @Column(columnDefinition = "TEXT")
    private String styleConfig; // JSON格式的样式配置

    /**
     * 宠主上传的宠物照片URL列表（JSON数组字符串）
     */
    @Column(columnDefinition = "TEXT")
    private String petPhotoUrls;

    /**
     * 设计协作状态
     * 0:草稿(未提交设计) 10:已提交服务端设计 20:服务端已回传设计稿 30:宠主已回传修改意见
     * 40:宠主已确认最终版 50:服务端已提交管理员审核 60:管理员已通过 61:管理员已拒绝
     */
    private Integer designStatus = 0;

    /**
     * 指派的服务人员ID（若订单有关联，可从订单带入）
     */
    private Long designProviderId;

    /**
     * 服务端回传的设计稿（图片URL JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    private String designDraftImages;

    /**
     * 服务端回传的设计稿PDF
     */
    @Column(length = 255)
    private String designDraftPdfUrl;

    /**
     * 宠主回传的修改稿（图片URL JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    private String ownerFeedbackImages;

    /**
     * 宠主回传的修改稿PDF
     */
    @Column(length = 255)
    private String ownerFeedbackPdfUrl;

    /**
     * 服务端最终版（图片URL JSON数组）
     */
    @Column(columnDefinition = "TEXT")
    private String designFinalImages;

    /**
     * 服务端最终版PDF
     */
    @Column(length = 255)
    private String designFinalPdfUrl;

    /**
     * 设计沟通备注（可选）
     */
    @Column(columnDefinition = "TEXT")
    private String designMessage;

    /**
     * 设计状态更新时间
     */
    private LocalDateTime designUpdateTime;

    /**
     * 管理员审核信息
     */
    private LocalDateTime adminReviewTime;

    @Column(length = 500)
    private String adminReviewComment;

    /**
     * 是否公开可分享
     */
    private Boolean isPublic = false;

    /**
     * 分享token（简化分享链接使用）
     */
    @Column(length = 64)
    private String shareToken;

    private Integer viewCount = 0;

    private Integer likeCount = 0;

    private Integer shareCount = 0;

    private Integer commentCount = 0;

    @Column(length = 255)
    private String pdfUrl;

    private Long pdfSize;

    private Long orderId;

    private LocalDateTime publishTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getPetType() {
        return petType;
    }

    public void setPetType(String petType) {
        this.petType = petType;
    }

    public LocalDateTime getPetBirthDate() {
        return petBirthDate;
    }

    public void setPetBirthDate(LocalDateTime petBirthDate) {
        this.petBirthDate = petBirthDate;
    }

    public LocalDateTime getPetDeathDate() {
        return petDeathDate;
    }

    public void setPetDeathDate(LocalDateTime petDeathDate) {
        this.petDeathDate = petDeathDate;
    }

    public String getPetMemory() {
        return petMemory;
    }

    public void setPetMemory(String petMemory) {
        this.petMemory = petMemory;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContentData() {
        return contentData;
    }

    public void setContentData(String contentData) {
        this.contentData = contentData;
    }

    public String getStyleConfig() {
        return styleConfig;
    }

    public void setStyleConfig(String styleConfig) {
        this.styleConfig = styleConfig;
    }

    public String getPetPhotoUrls() {
        return petPhotoUrls;
    }

    public void setPetPhotoUrls(String petPhotoUrls) {
        this.petPhotoUrls = petPhotoUrls;
    }

    public Integer getDesignStatus() {
        return designStatus;
    }

    public void setDesignStatus(Integer designStatus) {
        this.designStatus = designStatus;
    }

    public Long getDesignProviderId() {
        return designProviderId;
    }

    public void setDesignProviderId(Long designProviderId) {
        this.designProviderId = designProviderId;
    }

    public String getDesignDraftImages() {
        return designDraftImages;
    }

    public void setDesignDraftImages(String designDraftImages) {
        this.designDraftImages = designDraftImages;
    }

    public String getDesignDraftPdfUrl() {
        return designDraftPdfUrl;
    }

    public void setDesignDraftPdfUrl(String designDraftPdfUrl) {
        this.designDraftPdfUrl = designDraftPdfUrl;
    }

    public String getOwnerFeedbackImages() {
        return ownerFeedbackImages;
    }

    public void setOwnerFeedbackImages(String ownerFeedbackImages) {
        this.ownerFeedbackImages = ownerFeedbackImages;
    }

    public String getOwnerFeedbackPdfUrl() {
        return ownerFeedbackPdfUrl;
    }

    public void setOwnerFeedbackPdfUrl(String ownerFeedbackPdfUrl) {
        this.ownerFeedbackPdfUrl = ownerFeedbackPdfUrl;
    }

    public String getDesignFinalImages() {
        return designFinalImages;
    }

    public void setDesignFinalImages(String designFinalImages) {
        this.designFinalImages = designFinalImages;
    }

    public String getDesignFinalPdfUrl() {
        return designFinalPdfUrl;
    }

    public void setDesignFinalPdfUrl(String designFinalPdfUrl) {
        this.designFinalPdfUrl = designFinalPdfUrl;
    }

    public String getDesignMessage() {
        return designMessage;
    }

    public void setDesignMessage(String designMessage) {
        this.designMessage = designMessage;
    }

    public LocalDateTime getDesignUpdateTime() {
        return designUpdateTime;
    }

    public void setDesignUpdateTime(LocalDateTime designUpdateTime) {
        this.designUpdateTime = designUpdateTime;
    }

    public LocalDateTime getAdminReviewTime() {
        return adminReviewTime;
    }

    public void setAdminReviewTime(LocalDateTime adminReviewTime) {
        this.adminReviewTime = adminReviewTime;
    }

    public String getAdminReviewComment() {
        return adminReviewComment;
    }

    public void setAdminReviewComment(String adminReviewComment) {
        this.adminReviewComment = adminReviewComment;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getShareCount() {
        return shareCount;
    }

    public void setShareCount(Integer shareCount) {
        this.shareCount = shareCount;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public Long getPdfSize() {
        return pdfSize;
    }

    public void setPdfSize(Long pdfSize) {
        this.pdfSize = pdfSize;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}















