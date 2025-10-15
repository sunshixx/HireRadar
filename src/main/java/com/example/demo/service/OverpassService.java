package com.example.demo.service;

import com.example.demo.model.Place;
import com.example.demo.util.GeoUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OverpassService {
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 使用 Overpass（OSM）进行附近公司类 POI 检索。
     * <p>
     * 基于节点（node）进行周边搜索，支持关键词模糊匹配，返回标准化 Place 列表。
     * </p>
     *
     * @param lat          请求中心纬度（WGS-84）
     * @param lng          请求中心经度（WGS-84）
     * @param radiusMeters 半径（米）
     * @param keyword      关键词（可选）
     * @return Place 列表（按调用方排序）
     */
    public List<Place> searchNearby(double lat, double lng, int radiusMeters, String keyword) {
        String filter;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String safe = keyword.trim().replace("\"", "\\\"");
            filter = "[\"name\"~\"" + safe + "\",i]";
        } else {
            // General office POIs
            filter = "[\"office\"]";
        }

        String q = "[out:json][timeout:25];" +
                "node(around:" + radiusMeters + "," + lat + "," + lng + ")" + filter + ";" +
                "out 100;"; // limit to 100 results

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.USER_AGENT, "NearbyCompaniesDemo/1.0");
        String body = "data=" + URLEncoder.encode(q, StandardCharsets.UTF_8);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> resp = restTemplate.exchange(OVERPASS_URL, HttpMethod.POST, entity, JsonNode.class);
        JsonNode body = resp.getBody();
        List<Place> results = new ArrayList<>();
        if (body == null) return results;

        JsonNode elements = body.get("elements");
        if (elements != null && elements.isArray()) {
            for (JsonNode el : elements) {
                String type = el.path("type").asText("");
                if (!"node".equals(type)) continue; // only nodes for MVP

                String id = el.path("id").asText();
                double nLat = el.path("lat").asDouble(Double.NaN);
                double nLng = el.path("lon").asDouble(Double.NaN);
                if (Double.isNaN(nLat) || Double.isNaN(nLng)) continue;

                JsonNode tags = el.path("tags");
                String name = tags.path("name").asText("未命名企业");
                String address = buildAddress(tags);
                List<String> categories = new ArrayList<>();
                if (tags.has("office")) categories.add("office:" + tags.path("office").asText());
                if (tags.has("amenity")) categories.add("amenity:" + tags.path("amenity").asText());
                if (tags.has("industry")) categories.add("industry:" + tags.path("industry").asText());

                double distance = GeoUtils.haversineMeters(lat, lng, nLat, nLng);
                String url = "https://www.openstreetmap.org/node/" + id;

                results.add(new Place(id, name, address, nLat, nLng, distance, categories, "osm-overpass", url));
            }
        }
        return results;
    }

    /**
     * 构造地址字符串。
     *
     * @param tags OSM 标签
     * @return 拼接后的地址（可能为空字符串）
     */
    private String buildAddress(JsonNode tags) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, tags, "addr:province");
        appendIfPresent(sb, tags, "addr:city");
        appendIfPresent(sb, tags, "addr:district");
        appendIfPresent(sb, tags, "addr:street");
        appendIfPresent(sb, tags, "addr:housenumber");
        if (sb.length() == 0 && tags.has("addr:full")) {
            sb.append(tags.path("addr:full").asText());
        }
        return sb.toString();
    }

    /**
     * 若标签存在则追加至地址串。
     *
     * @param sb   地址构造器
     * @param tags OSM 标签
     * @param key  键名
     */
    private void appendIfPresent(StringBuilder sb, JsonNode tags, String key) {
        if (tags.has(key)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(tags.path(key).asText());
        }
    }
}