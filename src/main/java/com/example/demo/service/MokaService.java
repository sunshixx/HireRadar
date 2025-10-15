package com.example.demo.service;

import com.example.demo.model.JobLink;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Moka 招聘平台适配服务。
 * <p>
 * 支持两种鉴权方式：OAuth2 Client Credentials 与 Basic Auth（API Key），
 * 当配置了组织的职位接口路径时，优先调用官方 API 获取结构化职位与投递链接；
 * 未配置 API 时，回退提供 app.mokahr.com 的搜索链接作为线索。
 * </p>
 */
@Service
public class MokaService {
    private final RestTemplate restTemplate = new RestTemplate();

    /** 是否启用 Moka 适配 */
    @Value("${moka.enabled:false}")
    private boolean enabled;

    /** API 域名（中国版默认 api.mokahr.com；国际版 hire-r1-api.mokahr.com） */
    @Value("${moka.api.domain:api.mokahr.com}")
    private String apiDomain;

    /** 鉴权类型：oauth2 或 basic */
    @Value("${moka.auth:oauth2}")
    private String authType;

    /** Basic Auth 的 API Key（仅 username，password 为空） */
    @Value("${moka.api.key:}")
    private String apiKey;

    /** OAuth2 Client Credentials：clientId */
    @Value("${moka.clientId:}")
    private String clientId;

    /** OAuth2 Client Credentials：clientSecret */
    @Value("${moka.clientSecret:}")
    private String clientSecret;

    /** 组织职位接口路径（相对路径），例如：/api-platform/v1/jobs 或企业侧开放的职位列表 API */
    @Value("${moka.jobs.endpoint:}")
    private String jobsEndpoint;

    /**
     * 判断服务是否已满足调用官方 API 的必要配置。
     *
     * @return true 表示已启用且鉴权与职位接口路径有效
     */
    public boolean isConfigured() {
        if (!enabled || !StringUtils.hasText(jobsEndpoint)) {
            return false;
        }
        if ("basic".equalsIgnoreCase(authType)) {
            return StringUtils.hasText(apiKey);
        }
        // 默认按 oauth2
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }

    /**
     * 按公司名检索职位投递链接：优先官方 API，未配置则回退 app.mokahr.com 搜索链接。
     *
     * @param companyName 公司名称
     * @param limit       返回上限
     * @return 投递链接列表
     */
    public List<JobLink> searchByCompanyName(String companyName, int limit) {
        if (isConfigured()) {
            List<JobLink> apiList = fetchJobsViaApi(limit, companyName);
            if (!apiList.isEmpty()) {
                return apiList;
            }
        }
        // 回退：提供 app.mokahr.com 的搜索链接作为线索
        List<JobLink> fallback = new ArrayList<>();
        JobLink jl = new JobLink();
        jl.setTitle("Moka 搜索：" + companyName);
        jl.setUrl("https://app.mokahr.com/search?keyword=" + urlEncode(companyName));
        jl.setSource("moka");
        jl.setDescription("来源：Moka 搜索");
        jl.setCollectedAt(LocalDateTime.now());
        fallback.add(jl);
        return fallback;
    }

    /**
     * 调用官方职位接口，返回结构化投递链接（需要企业侧授权与职位接口路径）。
     *
     * @param limit       返回上限
     * @param companyName 可选关键词（若接口支持关键词检索）
     * @return 投递链接列表
     */
    private List<JobLink> fetchJobsViaApi(int limit, String companyName) {
        try {
            String url = "https://" + apiDomain + jobsEndpoint; // 由企业侧提供的职位接口相对路径
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, "HireRadar/1.0");
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            if ("basic".equalsIgnoreCase(authType)) {
                String basic = Base64.getEncoder().encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));
                headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
            } else {
                String token = getAccessToken();
                if (!StringUtils.hasText(token)) {
                    return List.of();
                }
                headers.setBearerAuth(token);
            }

            // 如果职位接口支持关键词，可按需添加查询参数（这里保持 GET 纯路径）
            ResponseEntity<JsonNode> resp = restTemplate.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            JsonNode body = resp.getBody();
            if (body == null) {
                return List.of();
            }
            // 兼容不同返回结构：jobs / data / items
            JsonNode arr = body.path("jobs");
            if (!arr.isArray() || arr.size() == 0) {
                arr = body.path("data");
            }
            if (!arr.isArray() || arr.size() == 0) {
                arr = body.path("items");
            }
            if (!arr.isArray() || arr.size() == 0) {
                return List.of();
            }
            List<JobLink> list = new ArrayList<>();
            for (int i = 0; i < arr.size() && list.size() < Math.max(1, limit); i++) {
                JsonNode j = arr.get(i);
                String title = firstText(j, "title", "name", "jobTitle");
                String apply = firstText(j, "applyUrl", "apply_url", "url", "jobUrl");
                if (!StringUtils.hasText(apply)) {
                    continue;
                }
                JobLink jl = new JobLink();
                jl.setTitle(StringUtils.hasText(title) ? title : "职位投递");
                jl.setUrl(apply);
                jl.setSource("moka");
                jl.setDescription("来源：Moka 官方接口");
                jl.setCollectedAt(LocalDateTime.now());
                list.add(jl);
            }
            return list;
        } catch (Exception ex) {
            // 接口不可用或配置不完整时返回空
            return List.of();
        }
    }

    /**
     * 获取 OAuth2 访问令牌（Client Credentials）。
     *
     * @return 访问令牌；失败返回空字符串
     */
    private String getAccessToken() {
        try {
            String url = "https://" + apiDomain + "/api-platform/v1/auth/oauth2/getToken";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.USER_AGENT, "HireRadar/1.0");
            String body = "{" +
                    "\"clientID\":\"" + jsonEscape(clientId) + "\"," +
                    "\"clientSecret\":\"" + jsonEscape(clientSecret) + "\"," +
                    "\"grantType\":\"client_credentials\"}";
            ResponseEntity<JsonNode> resp = restTemplate.exchange(URI.create(url), HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
            JsonNode node = resp.getBody();
            if (node == null) {
                return "";
            }
            String token = node.path("data").path("accessToken").asText("");
            return token;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 多字段兜底读取文本。
     *
     * @param n    节点
     * @param keys 备选字段
     * @return 第一个存在的非空文本
     */
    private String firstText(JsonNode n, String... keys) {
        for (String k : keys) {
            String v = n.path(k).asText("");
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return "";
    }

    /** URL 编码（UTF-8） */
    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    /** JSON 简易转义 */
    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}