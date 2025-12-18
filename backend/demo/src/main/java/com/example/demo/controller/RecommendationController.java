package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.PackageRecommendationRequestDTO;
import com.example.demo.service.RecommendationService;
import com.example.demo.vo.PackageRecommendationVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 智能套餐推荐（访客无需登录）
     */
    @PostMapping("/package")
    public Result<PackageRecommendationVO> recommendPackage(@RequestBody PackageRecommendationRequestDTO dto) {
        PackageRecommendationVO vo = recommendationService.recommendPackage(dto);
        return Result.success(vo);
    }
}



