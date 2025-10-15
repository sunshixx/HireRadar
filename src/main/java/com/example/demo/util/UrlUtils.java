package com.example.demo.util;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * URL 规范化工具。
 * <p>
 * 提供基础的 URL 规范化能力：小写协议/域名、移除末尾斜杠、查询参数排序与解码，便于做去重。
 * </p>
 */
public class UrlUtils {

    /**
     * 规范化 URL（不做网络请求）。
     *
     * @param url 原始 URL
     * @return 规范化后的 URL
     */
    public static String normalize(String url) {
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme() == null ? "https" : u.getScheme().toLowerCase();
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase();
            String path = u.getPath() == null ? "" : u.getPath();
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            String query = u.getQuery();
            if (query != null && !query.isEmpty()) {
                query = Arrays.stream(query.split("&"))
                        .map(p -> {
                            int i = p.indexOf('=');
                            if (i < 0) return decode(p) + "=";
                            return decode(p.substring(0, i)) + "=" + decode(p.substring(i + 1));
                        })
                        .sorted(Comparator.naturalOrder())
                        .map(UrlUtils::encode)
                        .collect(Collectors.joining("&"));
            }
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            sb.append(path);
            if (query != null && !query.isEmpty()) sb.append("?").append(query);
            return sb.toString();
        } catch (Exception e) {
            return url;
        }
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}