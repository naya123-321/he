package com.example.demo.dto;

/**
 * 访客/用户套餐推荐请求
 */
public class PackageRecommendationRequestDTO {
    private String petType;
    private Integer petAge;
    private String deathCause;

    public String getPetType() {
        return petType;
    }

    public void setPetType(String petType) {
        this.petType = petType;
    }

    public Integer getPetAge() {
        return petAge;
    }

    public void setPetAge(Integer petAge) {
        this.petAge = petAge;
    }

    public String getDeathCause() {
        return deathCause;
    }

    public void setDeathCause(String deathCause) {
        this.deathCause = deathCause;
    }
}


