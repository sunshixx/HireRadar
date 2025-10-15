package com.example.demo.service;

import com.example.demo.model.JobLink;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用模板爬虫。
 * <p>
 * 通过配置的 URL 模板（如：https://www.nowcoder.com/search?query=${name}），
 * 拉取 HTML 并基于关键字提取 A 标签投递链接，适合快速 MVP。
 * </p>
 */
public class GenericTemplateCrawler implements JobCrawler {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String templateUrl;
    private final String sourceName;
    private final List<String> keywords;

    /**
     * 构造函数。
     *
     * @param templateUrl URL 模板，包含占位符 ${name}
     * @param sourceName  数据源标识（站点名）
     * @param keywords    提取 A 标签文本/URL 的关键词列表（如：招聘、投递、职位、apply、career）
     */
    public GenericTemplateCrawler(String templateUrl, String sourceName, List<String> keywords) {
        this.templateUrl = templateUrl;
        this.sourceName = sourceName;
        this.keywords = keywords != null ? keywords : List.of("招聘", "投递", "职位", "校招", "社招", "apply", "career", "join");
    }

    @Override
    public List<JobLink> crawlByCompanyName(String companyName, int limit) {
        try {
            String url = templateUrl.replace("${name}", URLEncoder.encode(companyName, StandardCharsets.UTF_8));
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "NearbyCompaniesDemo/1.0");
            headers.setAccept(List.of(MediaType.TEXT_HTML));
            ResponseEntity<String> resp = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>(headers), String.class);
            String html = resp.getBody();
            if (!StringUtils.hasText(html)) {
                return List.of();
            }
            return extractLinks(html, limit);
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    /**
     * 基于关键词从 HTML 中提取 A 标签链接。
     *
     * @param html  HTML 文本
     * @param limit 返回条数上限
     * @return 投递链接列表
     */
    private List<JobLink> extractLinks(String html, int limit) {
        List<JobLink> list = new ArrayList<>();
        // 简单 A 标签提取（MVP），复杂场景建议替换为 Jsoup
        Pattern p = Pattern.compile("<a\\s+[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(html);
        while (m.find() && list.size() < Math.max(1, limit)) {
            String href = m.group(1);
            String text = m.group(2).replaceAll("<.*?>", "").trim();
            if (!StringUtils.hasText(href)) {
                continue;
            }
            String lower = (text + " " + href).toLowerCase();
            boolean match = keywords.stream().anyMatch(k -> lower.contains(k.toLowerCase()));
            if (!match) {
                continue;
            }
            JobLink jl = new JobLink();
            jl.setTitle(StringUtils.hasText(text) ? text : "投递链接");
            jl.setUrl(href);
            jl.setSource(sourceName);
            jl.setDescription("来源：" + sourceName);
            jl.setCollectedAt(LocalDateTime.now());
            list.add(jl);
        }
        return list;
    }
}