package com.example.demo.vo;

import java.util.List;

/**
 * 智能套餐推荐返回
 */
public class PackageRecommendationVO {
    private Long recommendedPackageId;
    private String recommendedPackageName;
    private Double score; // 0-100
    private Integer similarUsers;
    private List<String> analysis;
    private String algorithm;
    private String warning;

    public Long getRecommendedPackageId() {
        return recommendedPackageId;
    }

    public void setRecommendedPackageId(Long recommendedPackageId) {
        this.recommendedPackageId = recommendedPackageId;
    }

    public String getRecommendedPackageName() {
        return recommendedPackageName;
    }

    public void setRecommendedPackageName(String recommendedPackageName) {
        this.recommendedPackageName = recommendedPackageName;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getSimilarUsers() {
        return similarUsers;
    }

    public void setSimilarUsers(Integer similarUsers) {
        this.similarUsers = similarUsers;
    }

    public List<String> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(List<String> analysis) {
        this.analysis = analysis;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}


