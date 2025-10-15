package com.example.demo.service;

import com.example.demo.model.JobLink;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 投递链接聚合服务。
 * <p>
 * 通过多个爬虫实现聚合“职位/投递”链接，支持配置 URL 模板与关键词，
 * 并做去重、限量、按来源分组返回。
 * </p>
 */
@Service
public class JobLinkService {

    @Value("${jobs.crawler.templates:https://www.nowcoder.com/search?query=${name},https://www.offershow.cn/search?keyword=${name}}")
    private String templates;

    @Value("${jobs.crawler.keywords:招聘,投递,职位,校招,社招,apply,career,join}")
    private String keywords;

    @Value("${jobs.crawler.maxPerCompany:8}")
    private int maxPerCompany;

    /** 内存存储（演示用）：公司名 -> 链接列表 */
    private final Map<String, List<JobLink>> cache = new ConcurrentHashMap<>();

    private final MokaService mokaService;
    private final LinkSubmissionService linkSubmissionService;
    private final SitemapService sitemapService;
    private final JsonLdParser jsonLdParser;

    /**
     * 构造函数，注入各 ATS 服务与 Moka 服务。
     */
    public JobLinkService(MokaService mokaService,
                          LinkSubmissionService linkSubmissionService,
                          SitemapService sitemapService,
                          JsonLdParser jsonLdParser) {
        this.mokaService = mokaService;
        this.linkSubmissionService = linkSubmissionService;
        this.sitemapService = sitemapService;
        this.jsonLdParser = jsonLdParser;
    }

    /**
     * 根据公司名称检索投递链接并返回。
     * <p>
     * 若内存已缓存且不过期（演示不做过期），直接返回；否则按照模板抓取。
     * </p>
     *
     * @param companyName 公司名称
     * @return 聚合后的投递链接列表
     */
    public List<JobLink> searchLinks(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return List.of();
        }
        String key = normalizeName(companyName);
        List<JobLink> cached = cache.get(key);
        if (cached != null && !cached.isEmpty()) {
            return limit(cached, maxPerCompany);
        }
        List<JobCrawler> crawlers = buildCrawlers();
        List<JobLink> all = new ArrayList<>();
        for (JobCrawler c : crawlers) {
            try {
                List<JobLink> part = c.crawlByCompanyName(companyName, maxPerCompany);
                if (part != null && !part.isEmpty()) {
                    all.addAll(part);
                }
            } catch (Exception ignore) {
            }
        }
        // 合并审核通过的“提交投递链接”
        List<JobLink> submitted = linkSubmissionService.listApprovedApplyLinks(companyName);
        all.addAll(submitted);
        // 自动补充候选入口：基于官网域名映射 + sitemap/JSON-LD 抽取
        if (all.size() < maxPerCompany) {
            List<JobLink> candidates = extractCandidates(companyName, kw);
            all.addAll(candidates);
        }
        List<JobLink> deduped = dedupe(all);
        List<JobLink> limited = limit(deduped, maxPerCompany);
        cache.put(key, limited);
        return limited;
    }

    /** 公司域名映射（配置），格式示例：字节跳动=www.bytedance.com;阿里巴巴=www.alibaba.com */
    @org.springframework.beans.factory.annotation.Value("${jobs.domains:}")
    private String companyDomains;

    /**
     * 从配置域名映射加载官网域名。
     */
    private String findDomain(String companyName) {
        if (companyDomains == null || companyDomains.isBlank()) return "";
        String[] pairs = companyDomains.split(";");
        for (String p : pairs) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                if (companyName.equalsIgnoreCase(kv[0].trim())) {
                    return kv[1].trim();
                }
            }
        }
        return "";
    }

    /**
     * 基于官网 sitemap 与页面 JSON-LD 抽取候选投递入口。
     */
    private List<JobLink> extractCandidates(String companyName, List<String> kw) {
        String domain = findDomain(companyName);
        if (domain.isEmpty()) return java.util.List.of();
        try {
            java.util.List<String> urls = sitemapService.extractUrlsFromSitemap(domain);
            java.util.List<JobLink> list = new java.util.ArrayList<>();
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(3);
            java.util.List<java.util.concurrent.CompletableFuture<Void>> tasks = new java.util.ArrayList<>();
            for (String u : urls) {
                // 关键词预筛：减少请求量
                String lower = u.toLowerCase();
                boolean match = kw.stream().anyMatch(k -> lower.contains(k.toLowerCase())) || lower.contains("zhaopin") || lower.contains("jobs") || lower.contains("careers") || lower.contains("join");
                if (!match) continue;
                tasks.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                        headers.set(org.springframework.http.HttpHeaders.USER_AGENT, "HireRadar/1.0");
                        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
                        org.springframework.http.ResponseEntity<String> resp = rt.exchange(java.net.URI.create(u), org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers), String.class);
                        String html = resp.getBody();
                        if (html == null || html.isBlank()) return;
                        java.util.List<String> applyUrls = jsonLdParser.extractApplyUrls(html);
                        for (String au : applyUrls) {
                            JobLink jl = new JobLink();
                            jl.setTitle("投递入口");
                            jl.setUrl(com.example.demo.util.UrlUtils.normalize(au));
                            jl.setSource("sitemap/jsonld");
                            jl.setDescription("来源：官网 JSON-LD");
                            jl.setCollectedAt(java.time.LocalDateTime.now());
                            synchronized (list) { list.add(jl); }
                        }
                        // 若 JSON-LD 未命中，但页面 URL 符合关键词，也作为候选
                        JobLink jl2 = new JobLink();
                        jl2.setTitle("投递入口");
                        jl2.setUrl(com.example.demo.util.UrlUtils.normalize(u));
                        jl2.setSource("sitemap");
                        jl2.setDescription("来源：官网 sitemap");
                        jl2.setCollectedAt(java.time.LocalDateTime.now());
                        synchronized (list) { list.add(jl2); }
                    } catch (Exception ignore) {}
                }, pool));
                if (tasks.size() >= 6) break; // 控制请求量
            }
            java.util.concurrent.CompletableFuture.allOf(tasks.toArray(new java.util.concurrent.CompletableFuture[0])).join();
            pool.shutdown();
            return list;
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    /**
     * 构建爬虫实现。
     *
     * @return 爬虫列表
     */
    private List<JobCrawler> buildCrawlers() {
        List<String> tmpl = Arrays.stream(templates.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
        List<String> kw = Arrays.stream(keywords.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
        List<JobCrawler> list = new ArrayList<>();
        // 优先接入国内官方源（仅保留 Moka）
        list.add(new MokaCrawlerAdapter(mokaService));
        for (String t : tmpl) {
            String source = parseSourceName(t);
            list.add(new GenericTemplateCrawler(t, source, kw));
        }
        return list;
    }

    /**
     * 基于 URL 推断来源名。
     *
     * @param templateUrl 模板 URL
     * @return 站点名
     */
    private String parseSourceName(String templateUrl) {
        String s = templateUrl.replace("https://", "").replace("http://", "");
        int idx = s.indexOf('/');
        return idx > 0 ? s.substring(0, idx) : s;
    }

    /**
     * 去重：按 URL 去重。
     *
     * @param list 链接列表
     * @return 去重后列表
     */
    private List<JobLink> dedupe(List<JobLink> list) {
        Map<String, JobLink> map = new LinkedHashMap<>();
        for (JobLink jl : list) {
            if (jl.getUrl() == null) {
                continue;
            }
            map.putIfAbsent(jl.getUrl(), jl);
        }
        return new ArrayList<>(map.values());
    }

    /**
     * 限量返回。
     *
     * @param list  列表
     * @param limit 上限
     * @return 限量列表
     */
    private List<JobLink> limit(List<JobLink> list, int limit) {
        if (list == null) {
            return List.of();
        }
        return list.stream().limit(Math.max(1, limit)).collect(Collectors.toList());
    }

    /**
     * 名称归一化（去空格与常见后缀）。
     *
     * @param name 公司名
     * @return 归一化名称
     */
    private String normalizeName(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT);
        n = n.replace("有限公司", "").replace("有限责任公司", "").replace("股份有限公司", "");
        n = n.replace("公司", "").replace("集团", "");
        n = n.replaceAll("\\s+", "");
        return n;
    }
}