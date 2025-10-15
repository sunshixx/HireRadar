package com.example.demo.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 官网 sitemap 抽取服务（只抽链接）。
 */
@Service
public class SitemapService {
    private final RestTemplate rest = new RestTemplate();

    /**
     * 尝试读取 robots.txt 找到 Sitemap，再解析 sitemap.xml 抽取 URL。
     *
     * @param domain 公司官网域名，如 https://www.example.com
     * @return 站点 URL 列表
     */
    public List<String> extractUrlsFromSitemap(String domain) {
        try {
            String robotsUrl = normalize(domain) + "/robots.txt";
            HttpHeaders h = new HttpHeaders();
            h.set(HttpHeaders.USER_AGENT, "HireRadar/1.0");
            ResponseEntity<String> robotsResp = rest.exchange(URI.create(robotsUrl), HttpMethod.GET, new HttpEntity<>(h), String.class);
            String robots = robotsResp.getBody();
            if (!StringUtils.hasText(robots)) return List.of();
            String site = null;
            for (String line : robots.split("\n")) {
                line = line.trim();
                if (line.toLowerCase().startsWith("sitemap:")) {
                    site = line.substring(8).trim();
                    break;
                }
            }
            if (!StringUtils.hasText(site)) return List.of();
            ResponseEntity<String> smResp = rest.exchange(URI.create(site), HttpMethod.GET, new HttpEntity<>(h), String.class);
            String xml = smResp.getBody();
            if (!StringUtils.hasText(xml)) return List.of();
            return parseSitemapXml(xml);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 简单解析 sitemap XML 中的 <loc>URL</loc>。 */
    private List<String> parseSitemapXml(String xml) {
        try {
            var dbf = DocumentBuilderFactory.newInstance();
            var doc = dbf.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes()));
            var nodes = doc.getElementsByTagName("loc");
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                urls.add(nodes.item(i).getTextContent());
            }
            return urls;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalize(String domain) {
        if (domain.startsWith("http")) return domain;
        return "https://" + domain;
    }
}