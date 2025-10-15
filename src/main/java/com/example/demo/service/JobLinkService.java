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
        List<JobLink> deduped = dedupe(all);
        List<JobLink> limited = limit(deduped, maxPerCompany);
        cache.put(key, limited);
        return limited;
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