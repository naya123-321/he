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

        // 简单规则引擎：基于 petType/duration、age、deathCause、budget、participants 打分
        Map<Long, Double> popularity = computePopularity();
        ServiceType best = null;
        double bestScore = -1;
        Map<ServiceType, Double> allScores = new HashMap<>(); // 记录所有套餐的分数，用于调试
        
        for (ServiceType s : enabled) {
            double popularityScore = Math.min(20.0, popularity.getOrDefault(s.getId(), 0.0)); // 进一步降低历史热度权重
            double ruleScoreValue = ruleScore(dto, s);
            double totalScore = popularityScore + ruleScoreValue;
            
            allScores.put(s, totalScore);

            if (totalScore > bestScore) {
                bestScore = totalScore;
                best = s;
            }
        }

        // 如果所有套餐分数相同或非常接近，根据用户输入特征选择
        if (best == null) {
            best = enabled.get(0);
        } else {
            // 检查是否有多个套餐分数非常接近（差距小于3分）
            List<ServiceType> closeCandidates = new ArrayList<>();
            for (Map.Entry<ServiceType, Double> entry : allScores.entrySet()) {
                if (Math.abs(entry.getValue() - bestScore) < 3.0) {
                    closeCandidates.add(entry.getKey());
                }
            }
            
            if (closeCandidates.size() > 1 && dto != null) {
                // 根据用户输入的多个特征计算哈希值，确保不同输入选择不同套餐
                int hash = 0;
                if (dto.getPetType() != null) hash += dto.getPetType().hashCode() * 31;
                if (dto.getPetAge() != null) hash += dto.getPetAge() * 17;
                if (dto.getDeathCause() != null) hash += dto.getDeathCause().hashCode() * 13;
                if (dto.getBudgetMin() != null) hash += dto.getBudgetMin().intValue() * 7;
                if (dto.getBudgetMax() != null) hash += dto.getBudgetMax().intValue() * 5;
                if (dto.getParticipants() != null) hash += dto.getParticipants() * 3;
                
                int index = Math.abs(hash) % closeCandidates.size();
                best = closeCandidates.get(index);
                bestScore = allScores.get(best);
            }
        }

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
        double score = 0.0; // 从0开始，更清晰地计算各项得分

        // 1. 预算匹配（最重要，权重最高：0-30分）
        Double min = dto.getBudgetMin();
        Double max = dto.getBudgetMax();
        if (s.getPrice() != null) {
            double price = s.getPrice();
            // 未提供预算：中等分
            if (min == null && max == null) {
                score += 15;
            } else {
                double bmin = min == null ? 0.0 : min;
                double bmax = max == null ? Double.POSITIVE_INFINITY : max;
                if (min != null && max != null && bmax < bmin) {
                    double tmp = bmin;
                    bmin = bmax;
                    bmax = tmp;
                }

                // 预算目标价：略偏向预算上限的中高档，避免“总选最便宜”
                double target;
                if (min != null && max != null && Double.isFinite(bmax)) {
                    target = bmin + (bmax - bmin) * 0.7;
                } else if (max != null && Double.isFinite(bmax)) {
                    target = bmax * 0.85;
                } else if (min != null) {
                    target = bmin * 1.3;
                } else {
                    target = price;
                }

                // 预算分：允许在预算内更高分，明显偏离（尤其超预算）强扣分
                double budgetScore;
                if (max != null && Double.isFinite(bmax) && price > bmax) {
                    double overRatio = (price - bmax) / Math.max(1.0, bmax);
                    budgetScore = Math.max(-30.0, -30.0 * (0.6 + overRatio)); // 超预算：强惩罚
                } else if (min != null && price < bmin) {
                    double underRatio = (bmin - price) / Math.max(1.0, bmin);
                    budgetScore = Math.max(-18.0, -18.0 * underRatio); // 低于预算下限：惩罚（避免总选最便宜）
                } else {
                    double scale;
                    if (min != null && max != null && Double.isFinite(bmax)) {
                        scale = Math.max(1.0, (bmax - bmin) * 0.7);
                    } else if (max != null && Double.isFinite(bmax)) {
                        scale = Math.max(1.0, bmax * 0.85);
                    } else {
                        scale = Math.max(1.0, bmin * 0.9);
                    }
                    double closeness = 1.0 - Math.min(1.0, Math.abs(price - target) / scale);
                    budgetScore = 18.0 + 12.0 * closeness; // 18~30：预算内越贴近目标越高
                }

                score += budgetScore;
            }
        } else {
            // 无价格信息：只能给中等分
            score += 12;
        }

        // 2. 宠物类型 -> 服务时长匹配（0-20分）
        Integer duration = s.getDuration();
        String petType = safe(dto.getPetType()).toLowerCase();
        if (duration != null) {
            // 大型宠物（大型犬等）需要更长服务时长
            if (petType.contains("large") || petType.contains("大型")) {
                if (duration >= 90) score += 20;
                else if (duration >= 75) score += 12;
                else score += 5;
            } 
            // 中型宠物（中型犬等）
            else if (petType.contains("medium") || petType.contains("中型")) {
                if (duration >= 75) score += 18;
                else if (duration >= 60) score += 14;
                else score += 6;
            } 
            // 小型宠物（猫、小型犬、仓鼠、兔子、鸟等）
            else if (petType.contains("small") || petType.contains("小型") || 
                     petType.equals("cat") || petType.contains("猫") ||
                     petType.equals("hamster") || petType.contains("仓鼠") ||
                     petType.equals("rabbit") || petType.contains("兔子") ||
                     petType.equals("bird") || petType.contains("鸟") ||
                     petType.equals("chinchilla") || petType.contains("龙猫") ||
                     petType.equals("guinea-pig") || petType.equals("guinea-pig-2") ||
                     petType.equals("hedgehog") || petType.contains("刺猬") ||
                     petType.equals("ferret") || petType.contains("雪貂")) {
                if (duration <= 75) score += 18;
                else if (duration <= 90) score += 12;
                else score += 5;
            } 
            // 其他类型（鱼类、爬行动物等）
            else {
                if (duration >= 60 && duration <= 90) score += 15;
                else score += 8;
            }
        }

        // 3. 宠物年龄 -> 服务完整度（0-15分）
        Integer age = dto.getPetAge();
        if (age != null && s.getPrice() != null) {
            double median = medianPrice();
            if (age >= 8) {
                // 高龄宠物，建议更完整的服务
                if (s.getPrice() >= median) score += 15;
                else if (s.getPrice() >= median * 0.7) score += 10;
                else score += 5;
            } else if (age <= 2) {
                // 幼年宠物，可以选择性价比高的
                if (s.getPrice() <= median) score += 12;
                else score += 6;
            } else {
                // 中年宠物，中等分数
                score += 8;
            }
        }

        // 4. 离世原因 -> 服务温馨度（0-15分）
        String cause = safe(dto.getDeathCause()).toLowerCase();
        if (s.getPrice() != null) {
            double median = medianPrice();
            if (cause.contains("disease") || cause.contains("疾病")) {
                // 疾病离世，建议更温馨的服务
                if (s.getPrice() >= median) score += 15;
                else if (s.getPrice() >= median * 0.7) score += 10;
                else score += 6;
            } else if (cause.contains("accident") || cause.contains("意外")) {
                score += 10;
            } else if (cause.contains("aging") || cause.contains("衰老")) {
                if (s.getPrice() >= median) score += 12;
                else score += 8;
            } else {
                score += 8;
            }
        }

        // 5. 参与人数 -> 服务时长需求（0-10分）
        Integer participants = dto.getParticipants();
        if (participants != null && s.getDuration() != null) {
            if (participants >= 4) {
                // 参与人数多，需要更长服务时长
                if (s.getDuration() >= 90) score += 10;
                else if (s.getDuration() >= 75) score += 7;
                else score += 3;
            } else if (participants >= 2) {
                if (s.getDuration() >= 60) score += 8;
                else score += 5;
            } else {
                score += 5;
            }
        }

        return Math.max(0.0, Math.min(80.0, score)); // 规则分最高80分
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
        String s = v.toLowerCase().trim();
        // 精确匹配常见宠物类型
        if (s.equals("cat") || s.contains("猫")) return "猫";
        if (s.equals("dog") || s.contains("狗")) return "狗";
        if (s.equals("rabbit") || s.contains("兔子")) return "兔子";
        if (s.equals("hamster") || s.contains("仓鼠")) return "仓鼠";
        if (s.equals("bird") || s.contains("鸟")) return "鸟";
        if (s.equals("chinchilla") || s.contains("龙猫")) return "龙猫";
        if (s.equals("guinea-pig") || s.equals("guinea-pig-2") || s.contains("豚鼠") || s.contains("荷兰猪")) return "豚鼠";
        if (s.equals("hedgehog") || s.contains("刺猬")) return "刺猬";
        if (s.equals("ferret") || s.contains("雪貂")) return "雪貂";
        if (s.equals("reptile") || s.contains("爬行动物")) return "爬行动物";
        if (s.equals("fish") || s.contains("鱼类")) return "鱼类";
        // 模糊匹配（兼容旧数据）
        if (s.contains("dog_large") || s.contains("大型")) return "大型犬";
        if (s.contains("dog_medium") || s.contains("中型")) return "中型犬";
        if (s.contains("dog_small") || s.contains("小型")) return "小型犬";
        if (s.equals("other") || s.contains("其他")) return "其他";
        // 默认返回原始值（如果前端有映射，会进一步转换）
        return v;
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


