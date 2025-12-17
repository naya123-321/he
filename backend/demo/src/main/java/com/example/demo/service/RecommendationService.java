package com.example.demo.service;

import com.example.demo.dto.PackageRecommendationRequestDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.ServiceType;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ServiceTypeRepository;
import com.example.demo.vo.PackageRecommendationVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final OrderRepository orderRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final ObjectMapper objectMapper;

    @Value("${recommend.python.url:http://127.0.0.1:8001/recommend}")
    private String pythonRecommendUrl;

    @Value("${recommend.history.days:180}")
    private Integer historyDays;

    @Value("${recommend.similar.topk:256}")
    private Integer topK;

    public RecommendationService(OrderRepository orderRepository,
                                 ServiceTypeRepository serviceTypeRepository,
                                 ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.objectMapper = objectMapper;
    }

    public PackageRecommendationVO recommendPackage(PackageRecommendationRequestDTO dto) {
        // 基础校验
        if (dto == null) {
            return buildFallback(null, "请求为空，已使用降级推荐");
        }
        if (dto.getPetType() == null || dto.getPetType().trim().isEmpty()) {
            return buildFallback(dto, "缺少宠物类型，已使用降级推荐");
        }
        if (dto.getPetAge() == null || dto.getPetAge() < 0) {
            return buildFallback(dto, "缺少宠物年龄，已使用降级推荐");
        }
        if (dto.getDeathCause() == null || dto.getDeathCause().trim().isEmpty()) {
            return buildFallback(dto, "缺少离世原因，已使用降级推荐");
        }

        try {
            // 准备历史数据：最近 N 天订单
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(historyDays == null ? 180 : historyDays);
            List<Order> orders = orderRepository.findOrdersByDateRange(start, end);

            // 只保留有 serviceTypeId + userId 的记录
            List<Map<String, Object>> orderRows = orders.stream()
                    .filter(o -> o.getUserId() != null && o.getServiceTypeId() != null)
                    .map(o -> {
                        Map<String, Object> r = new HashMap<>();
                        r.put("userId", o.getUserId());
                        r.put("serviceTypeId", o.getServiceTypeId());
                        r.put("petType", o.getPetType());
                        r.put("petAge", o.getPetAge());
                        r.put("deathCause", o.getDeathCause());
                        return r;
                    })
                    .collect(Collectors.toList());

            // 服务套餐信息（启用）
            List<ServiceType> enabled = serviceTypeRepository.findByStatus(1);
            List<Map<String, Object>> serviceTypes = enabled.stream().map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", s.getId());
                m.put("name", s.getName());
                m.put("duration", s.getDuration());
                m.put("price", s.getPrice());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> profile = new HashMap<>();
            profile.put("petType", dto.getPetType());
            profile.put("petAge", dto.getPetAge());
            profile.put("deathCause", dto.getDeathCause());
            profile.put("budgetMin", dto.getBudgetMin());
            profile.put("budgetMax", dto.getBudgetMax());
            profile.put("participants", dto.getParticipants());
            payload.put("profile", profile);
            payload.put("orders", orderRows);
            payload.put("serviceTypes", serviceTypes);
            payload.put("topK", topK == null ? 256 : topK);

            String reqBody = objectMapper.writeValueAsString(payload);

            PythonCallResult resp = callPython(reqBody);
            if (resp.statusCode != 200) {
                String detail = buildPythonErrorDetail(resp.statusCode, resp.body, reqBody);
                String msg = "推荐服务返回异常(" + resp.statusCode + ")，已使用降级推荐";
                if (detail != null && !detail.isBlank()) {
                    msg = msg + "：" + detail;
                } else {
                    msg = msg + "（url=" + pythonRecommendUrl + "）";
                }
                return buildFallback(dto, msg);
            }

            JsonNode node = objectMapper.readTree(resp.body);
            PackageRecommendationVO vo = new PackageRecommendationVO();
            Long sid = node.path("recommendedServiceTypeId").isNumber() ? node.path("recommendedServiceTypeId").asLong() : null;
            vo.setRecommendedPackageId(sid);

            if (sid != null) {
                enabled.stream()
                        .filter(s -> Objects.equals(s.getId(), sid))
                        .findFirst()
                        .ifPresent(s -> vo.setRecommendedPackageName(s.getName()));
            }

            if (node.path("score").isNumber()) {
                vo.setScore(node.path("score").asDouble());
            }
            if (node.path("similarUsers").isNumber()) {
                vo.setSimilarUsers(node.path("similarUsers").asInt());
            }
            if (node.path("analysis").isArray()) {
                List<String> a = new ArrayList<>();
                node.path("analysis").forEach(x -> a.add(x.asText()));
                vo.setAnalysis(a);
            }
            if (node.path("algorithm").isTextual()) {
                vo.setAlgorithm(node.path("algorithm").asText());
            }
            if (node.path("warning").isTextual() && !node.path("warning").asText().isBlank()) {
                vo.setWarning(node.path("warning").asText());
            }

            // 若 Python 没返回 name，兜底补充
            if (vo.getRecommendedPackageName() == null && vo.getRecommendedPackageId() != null) {
                enabled.stream()
                        .filter(s -> Objects.equals(s.getId(), vo.getRecommendedPackageId()))
                        .findFirst()
                        .ifPresent(s -> vo.setRecommendedPackageName(s.getName()));
            }

            if (vo.getAlgorithm() == null || vo.getAlgorithm().isBlank()) {
                Integer su = vo.getSimilarUsers() == null ? 0 : vo.getSimilarUsers();
                vo.setAlgorithm("基于协同过滤算法，分析" + su + "个相似用户的选择");
            }

            // 如果推荐为空，降级
            if (vo.getRecommendedPackageId() == null) {
                return buildFallback(dto, "推荐服务未返回有效结果，已使用降级推荐");
            }
            return vo;
        } catch (Exception e) {
            return buildFallback(dto, "推荐服务不可用，已使用降级推荐（url=" + pythonRecommendUrl + "）：" + e.getMessage());
        }
    }

    private static class PythonCallResult {
        int statusCode;
        String body;
        PythonCallResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    /**
     * 先用 JDK HttpClient 调用；若出现 422/异常（部分环境下可能出现 body 传输兼容性问题），再用 RestTemplate 重试一次。
     */
    private PythonCallResult callPython(String reqBody) {
        // 1) JDK HttpClient
        try {
            byte[] reqBytes = reqBody.getBytes(StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonRecommendUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(reqBytes))
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                return new PythonCallResult(resp.statusCode(), resp.body());
            }
            // 422 很可能是入参校验/解析异常，直接走二次通道重试（以避免某些环境下 body 被解析为空）
            if (resp.statusCode() == 422) {
                PythonCallResult rt = callPythonByRestTemplate(reqBody);
                // 若重试成功则使用成功结果；否则返回第一次的结果（带更多 meta）
                if (rt.statusCode == 200) return rt;
            }
            return new PythonCallResult(resp.statusCode(), resp.body());
        } catch (Exception e) {
            // ignore, fallback to RestTemplate
        }

        // 2) RestTemplate fallback
        return callPythonByRestTemplate(reqBody);
    }

    private PythonCallResult callPythonByRestTemplate(String reqBody) {
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(3000);
            factory.setReadTimeout(8000);
            RestTemplate restTemplate = new RestTemplate(factory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/json; charset=UTF-8"));
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<String> entity = new HttpEntity<>(reqBody, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(pythonRecommendUrl, entity, String.class);
            int code = resp.getStatusCode().value();
            String body = resp.getBody() == null ? "" : resp.getBody();
            return new PythonCallResult(code, body);
        } catch (Exception e) {
            return new PythonCallResult(599, "{\"message\":\"RestTemplate调用失败: " + e.getMessage() + "\"}");
        }
    }

    private String buildPythonErrorDetail(int statusCode, String body, String reqBody) {
        if (body == null) return "";
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return "";
        // FastAPI 422 通常返回 {"detail":[{"loc":[...],"msg":"...","type":"..."}]}
        if (statusCode == 422) {
            try {
                JsonNode root = objectMapper.readTree(trimmed);
                JsonNode detail = root.path("detail");
                String metaText = "";
                JsonNode meta = root.path("meta");
                if (meta != null && meta.isObject()) {
                    String method = meta.path("method").isTextual() ? meta.path("method").asText() : "";
                    String path = meta.path("path").isTextual() ? meta.path("path").asText() : "";
                    String ct = meta.path("contentType").isTextual() ? meta.path("contentType").asText() : "";
                    String cl = meta.path("contentLength").isTextual() ? meta.path("contentLength").asText() : "";
                    String rawLen = meta.path("rawBodyLen").isNumber() ? String.valueOf(meta.path("rawBodyLen").asInt()) : "";
                    String rawPreview = meta.path("rawBodyPreview").isTextual() ? meta.path("rawBodyPreview").asText() : "";
                    List<String> parts = new ArrayList<>();
                    if (!method.isBlank() || !path.isBlank()) {
                        parts.add("method=" + (method.isBlank() ? "-" : method) + ", path=" + (path.isBlank() ? "-" : path));
                    }
                    if (!ct.isBlank() || !cl.isBlank()) {
                        parts.add("Content-Type=" + (ct.isBlank() ? "-" : ct) + ", Content-Length=" + (cl.isBlank() ? "-" : cl));
                    }
                    if (!rawLen.isBlank()) {
                        parts.add("rawBodyLen=" + rawLen);
                    }
                    if (!rawPreview.isBlank()) {
                        // 避免太长
                        String pv = rawPreview.length() > 80 ? rawPreview.substring(0, 80) + "..." : rawPreview;
                        parts.add("rawBodyPreview=" + pv.replaceAll("\\s+", " ").trim());
                    }
                    if (!parts.isEmpty()) {
                        metaText = "（" + String.join("; ", parts) + "）";
                    }
                }

                if (detail.isArray()) {
                    List<String> msgs = new ArrayList<>();
                    for (JsonNode it : detail) {
                        String loc = "";
                        if (it.path("loc").isArray()) {
                            List<String> locParts = new ArrayList<>();
                            it.path("loc").forEach(x -> locParts.add(x.asText()));
                            loc = String.join(".", locParts);
                        }
                        String msg = it.path("msg").isTextual() ? it.path("msg").asText() : "";
                        if (!loc.isBlank() && !msg.isBlank()) msgs.add(loc + ": " + msg);
                        else if (!msg.isBlank()) msgs.add(msg);
                    }
                    if (!msgs.isEmpty()) {
                        String reqInfo = "";
                        if (reqBody != null) {
                            reqInfo = "（requestBodyLen=" + reqBody.length() + "）";
                        }
                        return "参数校验失败：" + String.join("；", msgs) + metaText + reqInfo;
                    }
                }
                // 兜底：直接返回 root 文本
            } catch (Exception ignore) {
                // ignore
            }
        }

        // 其他状态：截断返回体，避免太长
        if (trimmed.length() > 220) {
            return trimmed.substring(0, 220) + "...";
        }
        return trimmed;
    }

    private PackageRecommendationVO buildFallback(PackageRecommendationRequestDTO dto, String warning) {
        List<ServiceType> enabled = serviceTypeRepository.findByStatus(1);
        PackageRecommendationVO vo = new PackageRecommendationVO();
        vo.setWarning(warning);

        if (enabled == null || enabled.isEmpty()) {
            vo.setAlgorithm("降级推荐（无可用套餐）");
            vo.setAnalysis(List.of("当前暂无可用套餐，请联系管理员"));
            vo.setScore(0.0);
            vo.setSimilarUsers(0);
            return vo;
        }

        // 简单规则引擎：基于 petType/duration、age、deathCause 打分
        Map<Long, Double> popularity = computePopularity();
        ServiceType best = null;
        double bestScore = -1;

        for (ServiceType s : enabled) {
            double score = 0;
            // 历史热度（0-50）
            score += Math.min(50.0, popularity.getOrDefault(s.getId(), 0.0));

            // 规则分（0-50）
            score += ruleScore(dto, s);

            if (score > bestScore) {
                bestScore = score;
                best = s;
            }
        }

        if (best == null) best = enabled.get(0);

        vo.setRecommendedPackageId(best.getId());
        vo.setRecommendedPackageName(best.getName());
        vo.setScore(Math.min(100.0, Math.max(1.0, bestScore)));
        vo.setSimilarUsers(estimateSimilarUsers(dto));
        vo.setAlgorithm("降级推荐（规则引擎 + 历史热度）");
        vo.setAnalysis(buildAnalysis(dto, best));
        return vo;
    }

    private Map<Long, Double> computePopularity() {
        // 用“订单量分布”粗略当做流行度
        try {
            List<Object[]> rows = orderRepository.countOrdersByServiceType();
            long max = 0;
            Map<Long, Long> counts = new HashMap<>();
            for (Object[] r : rows) {
                Long sid = ((Number) r[0]).longValue();
                Long c = ((Number) r[1]).longValue();
                counts.put(sid, c);
                max = Math.max(max, c);
            }
            Map<Long, Double> score = new HashMap<>();
            for (Map.Entry<Long, Long> e : counts.entrySet()) {
                if (max <= 0) score.put(e.getKey(), 0.0);
                else score.put(e.getKey(), (e.getValue() * 1.0 / max) * 50.0);
            }
            return score;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private double ruleScore(PackageRecommendationRequestDTO dto, ServiceType s) {
        if (dto == null) return 10.0;
        double score = 10.0;

        // petType -> duration 偏好
        Integer duration = s.getDuration();
        String petType = safe(dto.getPetType());
        if (duration != null) {
            if (petType.contains("large") || petType.contains("大型")) {
                if (duration >= 90) score += 18;
            } else if (petType.contains("medium") || petType.contains("中型")) {
                if (duration >= 75) score += 16;
            } else if (petType.contains("small") || petType.contains("小型") || petType.contains("cat") || petType.contains("猫")) {
                if (duration <= 75) score += 14;
            } else {
                score += 8;
            }
        }

        // age -> 更倾向纪念册/更完整流程（用“价格/时长”粗略近似）
        Integer age = dto.getPetAge();
        if (age != null) {
            if (age >= 8) {
                if (s.getPrice() != null && s.getPrice() >= medianPrice()) score += 14;
            } else if (age <= 2) {
                if (s.getPrice() != null && s.getPrice() <= medianPrice()) score += 10;
            } else {
                score += 6;
            }
        }

        // deathCause -> 疾病更温馨（偏中高端）
        String cause = safe(dto.getDeathCause());
        if (cause.contains("disease") || cause.contains("疾病")) {
            if (s.getPrice() != null && s.getPrice() >= medianPrice()) score += 12;
        } else if (cause.contains("accident") || cause.contains("意外")) {
            score += 8;
        } else {
            score += 6;
        }

        // budget -> 预算适配
        Double min = dto.getBudgetMin();
        Double max = dto.getBudgetMax();
        if (s.getPrice() != null && (min != null || max != null)) {
            if (min != null && s.getPrice() < min) {
                score -= 8;
            } else if (max != null && s.getPrice() > max) {
                score -= 12;
            } else {
                score += 10;
            }
        }

        // participants -> 参与人数较多倾向更长服务时长/更完整流程
        Integer participants = dto.getParticipants();
        if (participants != null && participants >= 4) {
            if (s.getDuration() != null && s.getDuration() >= 90) score += 10;
            else if (s.getDuration() != null && s.getDuration() >= 75) score += 6;
        }

        return Math.min(50.0, score);
    }

    private double medianPrice() {
        List<ServiceType> enabled = serviceTypeRepository.findByStatus(1);
        if (enabled == null || enabled.isEmpty()) return 0;
        List<Double> prices = enabled.stream()
                .map(ServiceType::getPrice)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        if (prices.isEmpty()) return 0;
        int mid = prices.size() / 2;
        if (prices.size() % 2 == 1) return prices.get(mid);
        return (prices.get(mid - 1) + prices.get(mid)) / 2.0;
    }

    private List<String> buildAnalysis(PackageRecommendationRequestDTO dto, ServiceType best) {
        List<String> a = new ArrayList<>();
        if (dto == null) return a;
        String petTypeText = petTypeText(dto.getPetType());
        String causeText = deathCauseText(dto.getDeathCause());
        Integer age = dto.getPetAge();
        Integer participants = dto.getParticipants();
        Double min = dto.getBudgetMin();
        Double max = dto.getBudgetMax();

        String dur = best.getDuration() == null ? "适中" : (best.getDuration() + "分钟");
        a.add("宠物类型：" + petTypeText + " → 适合" + dur + "服务");
        if (age != null) {
            if (age >= 8) a.add("宠物年龄：" + age + "岁 → 建议选择更完整的告别与纪念服务");
            else a.add("宠物年龄：" + age + "岁 → 建议选择舒适且高性价比的服务组合");
        }
        a.add("离世原因：" + causeText + " → 侧重更温馨的告别仪式与陪伴体验");
        if (participants != null) {
            a.add("参与告别人数：" + participants + "人 → 建议选择更从容的告别流程与时间安排");
        }
        if (min != null || max != null) {
            a.add("预算范围：" + (min == null ? "0" : String.valueOf(Math.round(min))) + " - " + (max == null ? "不限" : String.valueOf(Math.round(max))) + "元 → 优先匹配预算内的套餐");
        }
        return a;
    }

    private int estimateSimilarUsers(PackageRecommendationRequestDTO dto) {
        // 简单估算：近 historyDays 订单中 petType + age bucket + deathCause 命中数量
        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusDays(historyDays == null ? 180 : historyDays);
            List<Order> orders = orderRepository.findOrdersByDateRange(start, end);
            if (orders == null || orders.isEmpty() || dto == null) return 0;
            String pt = safe(dto.getPetType());
            String dc = safe(dto.getDeathCause());
            int ab = ageBucket(dto.getPetAge());
            Set<Long> userIds = new HashSet<>();
            for (Order o : orders) {
                if (o.getUserId() == null) continue;
                if (!pt.isBlank() && safe(o.getPetType()).equalsIgnoreCase(pt)) {
                    if (ageBucket(o.getPetAge()) == ab) {
                        if (!dc.isBlank() && safe(o.getDeathCause()).equalsIgnoreCase(dc)) {
                            userIds.add(o.getUserId());
                        }
                    }
                }
            }
            return Math.min(topK == null ? 256 : topK, userIds.size());
        } catch (Exception e) {
            return 0;
        }
    }

    private int ageBucket(Integer age) {
        if (age == null) return -1;
        if (age <= 1) return 0;
        if (age <= 5) return 1;
        if (age <= 10) return 2;
        return 3;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String petTypeText(String v) {
        if (v == null) return "未知";
        String s = v.toLowerCase();
        if (s.contains("cat") || s.contains("猫")) return "猫";
        if (s.contains("dog_large") || s.contains("大型")) return "大型犬";
        if (s.contains("dog_medium") || s.contains("中型")) return "中型犬";
        if (s.contains("dog_small") || s.contains("小型")) return "小型犬";
        if (s.contains("dog")) return "犬";
        return "其他";
    }

    private String deathCauseText(String v) {
        if (v == null) return "未知";
        String s = v.toLowerCase();
        if (s.contains("disease") || s.contains("疾病")) return "疾病";
        if (s.contains("accident") || s.contains("意外")) return "意外";
        if (s.contains("aging") || s.contains("衰老")) return "自然衰老";
        return "其他";
    }
}


