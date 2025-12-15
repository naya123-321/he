package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MemorialCreateDTO {
    private String title;
    private String subtitle;
    private Long templateId;
    private Long orderId;
    private String petName;
    private String petType;
    
    // 直接使用字符串字段接收日期
    private String petBirthDate;
    
    private String petDeathDate;
    
    private String petMemory;
    
    // 获取转换后的LocalDateTime（供Service层使用）
    public LocalDateTime getPetBirthDateAsDateTime() {
        if (petBirthDate == null || petBirthDate.isEmpty()) {
            return null;
        }
        try {
            // 尝试解析完整格式 yyyy-MM-ddTHH:mm:ss
            if (petBirthDate.contains("T")) {
                return LocalDateTime.parse(petBirthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            }
            // 否则解析日期格式 yyyy-MM-dd，并添加时间 00:00:00
            LocalDate date = LocalDate.parse(petBirthDate);
            return date.atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }
    
    public LocalDateTime getPetDeathDateAsDateTime() {
        if (petDeathDate == null || petDeathDate.isEmpty()) {
            return null;
        }
        try {
            // 尝试解析完整格式 yyyy-MM-ddTHH:mm:ss
            if (petDeathDate.contains("T")) {
                return LocalDateTime.parse(petDeathDate, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            }
            // 否则解析日期格式 yyyy-MM-dd，并添加时间 00:00:00
            LocalDate date = LocalDate.parse(petDeathDate);
            return date.atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    // Getters and Setters
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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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

    public String getPetBirthDate() {
        return petBirthDate;
    }

    public void setPetBirthDate(String petBirthDate) {
        this.petBirthDate = petBirthDate;
    }

    public String getPetDeathDate() {
        return petDeathDate;
    }

    public void setPetDeathDate(String petDeathDate) {
        this.petDeathDate = petDeathDate;
    }

    public String getPetMemory() {
        return petMemory;
    }

    public void setPetMemory(String petMemory) {
        this.petMemory = petMemory;
    }
}
