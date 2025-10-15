package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON-LD 解析器（只抽取 JobPosting/Organization 的 URL/投递入口）。
 */
@Service
public class JsonLdParser {
    private static final Pattern JSON_LD_PATTERN = Pattern.compile("<script[^>]*type=\\\"application/ld\\+json\\\"[^>]*>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 从 HTML 中提取 JSON-LD，并抽取投递相关链接。
     *
     * @param html HTML 文本
     * @return 可能的投递/职位/组织 URL 列表
     */
    public List<String> extractApplyUrls(String html) {
        List<String> urls = new ArrayList<>();
        Matcher m = JSON_LD_PATTERN.matcher(html);
        while (m.find()) {
            String json = m.group(1);
            try {
                JsonNode node = mapper.readTree(json);
                collectUrls(node, urls);
            } catch (Exception ignore) {}
        }
        return urls;
    }

    private void collectUrls(JsonNode node, List<String> urls) {
        if (node == null) return;
        if (node.isArray()) {
            for (JsonNode n : node) collectUrls(n, urls);
            return;
        }
        String type = node.path("@type").asText("");
        if ("JobPosting".equalsIgnoreCase(type)) {
            addIfPresent(urls, node.path("url").asText(""));
            addIfPresent(urls, node.path("applicationUrl").asText(""));
        } else if ("Organization".equalsIgnoreCase(type)) {
            addIfPresent(urls, node.path("url").asText(""));
        }
        // 深度遍历可能的 graph 或 nested
        collectUrls(node.path("@graph"), urls);
    }

    private void addIfPresent(List<String> urls, String u) {
        if (StringUtils.hasText(u)) {
            urls.add(u);
        }
    }
}