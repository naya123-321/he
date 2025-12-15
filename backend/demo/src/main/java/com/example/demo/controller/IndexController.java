package com.example.demo.controller;

import com.example.demo.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 根路径控制器
 */
@RestController
public class IndexController {

    @GetMapping("/")
    public Result<Map<String, Object>> index() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Pet Memorial API");
        info.put("version", "1.0.0");
        info.put("status", "running");
        info.put("port", 8082);
        info.put("endpoints", Map.of(
            "auth", "/api/auth",
            "order", "/api/order",
            "memorial", "/api/memorial",
            "service-type", "/api/service-type"
        ));
        return Result.success(info);
    }

    @GetMapping("/api")
    public Result<Map<String, Object>> api() {
        Map<String, Object> info = new HashMap<>();
        info.put("message", "Pet Memorial API Server");
        info.put("endpoints", Map.of(
            "auth", "/api/auth",
            "order", "/api/order",
            "memorial", "/api/memorial",
            "service-type", "/api/service-type"
        ));
        return Result.success(info);
    }
}















