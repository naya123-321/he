package com.example.demo.vo;

import java.time.LocalDateTime;
import java.util.List;

public class MemorialVO {
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private String title;
    private String subtitle;
    private String coverImage;
    private Long templateId;
    private String templateName;
    private String templateCategory;
    private String petName;
    private String petType;
    private LocalDateTime petBirthDate;
    private LocalDateTime petDeathDate;
    private String petMemory;
    private Integer status;
    private String statusText;
    private Integer viewCount;
    private Integer likeCount;
    private Integer shareCount;
    private Integer commentCount;
    private Boolean isFavorite;
    private Boolean isOwner;
    private String pdfUrl;
    private Long pdfSize;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private Object previewContent;

    /**
     * 关联订单ID（用于设计协作分配服务人员）
     */
    private Long orderId;

    // ===== 设计协作相关 =====
    private Integer designStatus;
    private String designStatusText;
    private Long designProviderId;
    private String designProviderName;

    private List<String> petPhotoUrls;

    private List<String> designDraftImages;
    private String designDraftPdfUrl;

    private List<String> ownerFeedbackImages;
    private String ownerFeedbackPdfUrl;

    private List<String> designFinalImages;
    private String designFinalPdfUrl;

    private String designMessage;

    private LocalDateTime designUpdateTime;
    private LocalDateTime adminReviewTime;
    private String adminReviewComment;

    private Boolean isPublic;
    private String shareToken;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateCategory() {
        return templateCategory;
    }

    public void setTemplateCategory(String templateCategory) {
        this.templateCategory = templateCategory;
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

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
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

    public Boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Boolean getIsOwner() {
        return isOwner;
    }

    public void setIsOwner(Boolean isOwner) {
        this.isOwner = isOwner;
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

    public Object getPreviewContent() {
        return previewContent;
    }

    public void setPreviewContent(Object previewContent) {
        this.previewContent = previewContent;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getDesignStatus() {
        return designStatus;
    }

    public void setDesignStatus(Integer designStatus) {
        this.designStatus = designStatus;
    }

    public String getDesignStatusText() {
        return designStatusText;
    }

    public void setDesignStatusText(String designStatusText) {
        this.designStatusText = designStatusText;
    }

    public Long getDesignProviderId() {
        return designProviderId;
    }

    public void setDesignProviderId(Long designProviderId) {
        this.designProviderId = designProviderId;
    }

    public String getDesignProviderName() {
        return designProviderName;
    }

    public void setDesignProviderName(String designProviderName) {
        this.designProviderName = designProviderName;
    }

    public List<String> getPetPhotoUrls() {
        return petPhotoUrls;
    }

    public void setPetPhotoUrls(List<String> petPhotoUrls) {
        this.petPhotoUrls = petPhotoUrls;
    }

    public List<String> getDesignDraftImages() {
        return designDraftImages;
    }

    public void setDesignDraftImages(List<String> designDraftImages) {
        this.designDraftImages = designDraftImages;
    }

    public String getDesignDraftPdfUrl() {
        return designDraftPdfUrl;
    }

    public void setDesignDraftPdfUrl(String designDraftPdfUrl) {
        this.designDraftPdfUrl = designDraftPdfUrl;
    }

    public List<String> getOwnerFeedbackImages() {
        return ownerFeedbackImages;
    }

    public void setOwnerFeedbackImages(List<String> ownerFeedbackImages) {
        this.ownerFeedbackImages = ownerFeedbackImages;
    }

    public String getOwnerFeedbackPdfUrl() {
        return ownerFeedbackPdfUrl;
    }

    public void setOwnerFeedbackPdfUrl(String ownerFeedbackPdfUrl) {
        this.ownerFeedbackPdfUrl = ownerFeedbackPdfUrl;
    }

    public List<String> getDesignFinalImages() {
        return designFinalImages;
    }

    public void setDesignFinalImages(List<String> designFinalImages) {
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
}















