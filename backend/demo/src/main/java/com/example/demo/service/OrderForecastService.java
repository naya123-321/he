package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.entity.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Service
public class OrderForecastService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OrderRepository orderRepository;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${forecast.python.command:python}")
    private String pythonCmd;

    @Value("${forecast.order.history-days:60}")
    private Integer defaultHistoryDays;

    @Value("${forecast.cache.minutes:30}")
    private Integer cacheMinutes;

    public Map<String, Object> getOrderForecast(Integer days, Integer historyDays, Boolean force) {
        int d = (days == null || days <= 0) ? 3 : days;
        int h = (historyDays == null || historyDays <= 0) ? defaultHistoryDays : historyDays;
        boolean f = force != null && force;

        Path baseDir = resolveBaseDir();
        Path outDir = baseDir.resolve("data");
        Path outFile = outDir.resolve("order_forecast.json");
        try {
            Files.createDirectories(outDir);
        } catch (Exception ignored) {}

        if (!f && Files.exists(outFile)) {
            try {
                Instant last = Files.getLastModifiedTime(outFile).toInstant();
                if (Duration.between(last, Instant.now()).toMinutes() < cacheMinutes) {
                    return readJson(outFile);
                }
            } catch (Exception ignored) {}
        }

        // 运行 Python 生成 JSON，失败则尝试返回旧文件
        try {
            runPythonForecast(baseDir, outFile, d, h);
            return readJson(outFile);
        } catch (Exception e) {
            try {
                if (Files.exists(outFile)) {
                    Map<String, Object> cached = readJson(outFile);
                    cached.put("warning", "预测脚本执行失败，返回缓存结果: " + e.getMessage());
                    return cached;
                }
            } catch (Exception ignored) {}
            // 无缓存：降级为数据库简单预测，保证页面不报错
            return buildFallbackForecast(d, h, "Python依赖缺失或执行失败，已降级为简单预测：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(Path file) throws Exception {
        return (Map<String, Object>) objectMapper.readValue(file.toFile(), Map.class);
    }

    private void runPythonForecast(Path baseDir, Path outFile, int days, int historyDays) throws Exception {
        Path script = baseDir.resolve("scripts").resolve("order_arima_forecast.py");
        if (!Files.exists(script)) {
            throw new RuntimeException("预测脚本不存在: " + script);
        }

        List<List<String>> candidates = new ArrayList<>();
        List<String> configured = splitCommand(pythonCmd);
        if (!configured.isEmpty()) candidates.add(configured);
        // 如果配置是默认 python，额外尝试 Windows 常见入口（py -3 / py）
        if (configured.isEmpty() || (configured.size() == 1 && "python".equalsIgnoreCase(configured.get(0)))) {
            candidates.add(List.of("py", "-3"));
            candidates.add(List.of("py"));
            candidates.add(List.of("python"));
        }

        Exception last = null;
        String preflightInfo = null;
        List<String> python = null;
        for (List<String> cand : candidates) {
            try {
                preflightInfo = preflightPython(baseDir, cand);
                python = cand;
                break;
            } catch (Exception e) {
                last = e;
            }
        }
        if (python == null) {
            throw new RuntimeException(last != null ? last.getMessage() : "Python环境预检失败");
        }

        List<String> cmd = new ArrayList<>(python);
        cmd.add(script.toString());
        cmd.add("--jdbc-url");
        cmd.add(jdbcUrl);
        cmd.add("--username");
        cmd.add(dbUsername);
        cmd.add("--password");
        cmd.add(dbPassword);
        cmd.add("--days");
        cmd.add(String.valueOf(days));
        cmd.add("--history-days");
        cmd.add(String.valueOf(historyDays));
        cmd.add("--out");
        cmd.add(outFile.toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(baseDir.toString()));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append("\n");
            }
        }
        int code = p.waitFor();
        if (code != 0) {
            String msg = "Python脚本退出码=" + code + " 输出: " + truncate(out.toString(), 1200);
            if (preflightInfo != null && !preflightInfo.isEmpty()) {
                msg = preflightInfo + "\n" + msg;
            }
            throw new RuntimeException(msg);
        }
        if (!Files.exists(outFile)) {
            throw new RuntimeException("预测结果文件未生成: " + outFile);
        }
    }

    /**
     * 预检通过则返回包含 PY_EXE/PY_VER 的信息字符串；失败抛异常（消息包含输出）。
     */
    private String preflightPython(Path baseDir, List<String> python) throws Exception {
        // 打印 sys.executable 并验证依赖是否可 import
        String code = ""
                + "import sys\n"
                + "print('PY_EXE=' + sys.executable)\n"
                + "print('PY_VER=' + sys.version.replace('\\n',' '))\n"
                + "import numpy, pandas, statsmodels, pymysql\n"
                + "print('OK numpy=' + getattr(numpy,'__version__','?'))\n"
                + "print('OK pandas=' + getattr(pandas,'__version__','?'))\n"
                + "print('OK statsmodels=' + getattr(statsmodels,'__version__','?'))\n"
                + "print('OK pymysql=' + getattr(pymysql,'__version__','?'))\n";

        List<String> cmd = new ArrayList<>(python);
        cmd.add("-c");
        cmd.add(code);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(baseDir.toString()));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                out.append(line).append("\n");
            }
        }
        int codeExit = p.waitFor();
        if (codeExit != 0) {
            throw new RuntimeException(
                    "Python环境预检失败（后端实际调用命令=" + String.join(" ", python) + "）输出: "
                            + truncate(out.toString(), 1200)
                            + "；请确保在该解释器环境执行：python -m pip install -r backend/demo/scripts/requirements.txt"
            );
        }
        return "Python环境预检通过（命令=" + String.join(" ", python) + "）\n" + truncate(out.toString(), 600);
    }

    private List<String> splitCommand(String command) {
        // 支持：py -3、"C:\Path With Space\python.exe" 等
        if (command == null) return List.of();
        String s = command.trim();
        if (s.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '"' || c == '\'') && !inQuote) {
                inQuote = true;
                quote = c;
                continue;
            }
            if (inQuote && c == quote) {
                inQuote = false;
                continue;
            }
            if (!inQuote && Character.isWhitespace(c)) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                continue;
            }
            cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }

    /**
     * 兼容两种启动方式：
     * - 在 backend/demo 目录启动（user.dir=.../backend/demo）
     * - 在项目根目录启动（user.dir=.../项目根），此时脚本位于 backend/demo/scripts
     */
    private Path resolveBaseDir() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        // 直接命中
        if (Files.exists(cwd.resolve("scripts").resolve("order_arima_forecast.py"))) {
            return cwd;
        }
        // 在 backend 目录启动（user.dir=.../backend）
        // 脚本位于 backend/demo/scripts
        if (cwd.getFileName() != null && "backend".equalsIgnoreCase(cwd.getFileName().toString())) {
            Path candidate = cwd.resolve("demo");
            if (Files.exists(candidate.resolve("scripts").resolve("order_arima_forecast.py"))) {
                return candidate;
            }
        }
        // 从项目根目录启动
        Path candidate = cwd.resolve("backend").resolve("demo");
        if (Files.exists(candidate.resolve("scripts").resolve("order_arima_forecast.py"))) {
            return candidate;
        }
        // 兜底返回 cwd，让报错信息更直观
        return cwd;
    }

    /**
     * 降级预测：使用最近 N 天订单量的 7 日均值 + 95%区间（均值±1.96*std）
     */
    private Map<String, Object> buildFallbackForecast(int days, int historyDays, String warning) {
        Map<String, Object> out = new HashMap<>();
        out.put("generatedAt", java.time.LocalDateTime.now().toString());
        out.put("historyDays", historyDays);
        Map<String, Object> model = new HashMap<>();
        model.put("type", "FALLBACK");
        model.put("confidence", 0.95);
        out.put("model", model);
        out.put("warning", warning);

        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(Math.max(1, historyDays - 1));
        LocalDateTime start = startDay.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay().minusSeconds(1);

        List<Order> orders = orderRepository.findOrdersByDateRange(start, end);
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (Order o : orders) {
            LocalDate d = o.getCreateTime() != null ? o.getCreateTime().toLocalDate() : null;
            if (d != null) counts.put(d, counts.getOrDefault(d, 0) + 1);
        }

        // 生成最近 30 天 history（给前端展示/调试）
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        java.util.ArrayList<Map<String, Object>> history = new java.util.ArrayList<>();
        int histShow = 30;
        LocalDate histStart = today.minusDays(histShow - 1);
        for (int i = 0; i < histShow; i++) {
            LocalDate day = histStart.plusDays(i);
            Map<String, Object> p = new HashMap<>();
            p.put("date", day.format(df));
            p.put("count", counts.getOrDefault(day, 0));
            history.add(p);
        }
        out.put("history", history);

        // 计算 7 日均值和标准差
        int window = 7;
        java.util.ArrayList<Double> last = new java.util.ArrayList<>();
        for (int i = window - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            last.add((double) counts.getOrDefault(day, 0));
        }
        double mean = last.stream().mapToDouble(x -> x).average().orElse(0.0);
        double var = 0.0;
        for (double x : last) var += (x - mean) * (x - mean);
        var /= Math.max(1, last.size());
        double std = Math.sqrt(var);

        java.util.ArrayList<Map<String, Object>> forecast = new java.util.ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = today.plusDays(i + 1);
            int pred = (int) Math.max(0, Math.round(mean));
            int lower = (int) Math.max(0, Math.round(mean - 1.96 * std));
            int upper = (int) Math.max(lower, Math.round(mean + 1.96 * std));
            Map<String, Object> p = new HashMap<>();
            p.put("date", day.format(df));
            p.put("predicted", pred);
            p.put("lower", lower);
            p.put("upper", upper);
            forecast.add(p);
        }
        out.put("forecast", forecast);
        return out;
    }
}


