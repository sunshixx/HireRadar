package com.example.demo.service;

import com.example.demo.model.Place;
import com.example.demo.util.CoordTransform;
import com.example.demo.util.GeoUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AmapService {
    private static final String AMAP_PLACE_AROUND = "https://restapi.amap.com/v3/place/around";

    @Value("${map.amap.key:}")
    private String amapKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 判断是否已配置高德密钥。
     *
     * @return true 已配置；false 未配置
     */
    public boolean isConfigured() {
        return amapKey != null && !amapKey.isBlank();
    }

    /**
     * 使用高德周边搜索进行附近公司检索。
     * <p>
     * 将传入的 WGS-84 坐标转换为 GCJ-02 后调用高德接口；并将返回结果坐标再转换回 WGS-84。
     * </p>
     *
     * @param wgsLat       纬度（WGS-84）
     * @param wgsLng       经度（WGS-84）
     * @param radiusMeters 半径（米）
     * @param keyword      关键词（可选）
     * @return 标准化 Place 列表
     */
    public List<Place> searchNearby(double wgsLat, double wgsLng, int radiusMeters, String keyword) {
        if (!isConfigured()) return List.of();

        double[] gcj = CoordTransform.wgsToGcj(wgsLat, wgsLng);
        double gcjLat = gcj[0];
        double gcjLng = gcj[1];

        StringBuilder keywords = new StringBuilder("公司|企业");
        if (keyword != null && !keyword.trim().isEmpty()) {
            keywords.append("|").append(keyword.trim());
        }

        String qs = "key=" + URLEncoder.encode(amapKey, StandardCharsets.UTF_8) +
                "&location=" + gcjLng + "," + gcjLat +
                "&radius=" + radiusMeters +
                "&keywords=" + URLEncoder.encode(keywords.toString(), StandardCharsets.UTF_8) +
                "&offset=25&extensions=base&output=json";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.USER_AGENT, "NearbyCompaniesDemo/1.0");
        HttpEntity<String> entity = new HttpEntity<>(qs, headers);

        ResponseEntity<JsonNode> resp = restTemplate.exchange(AMAP_PLACE_AROUND, HttpMethod.POST, entity, JsonNode.class);
        JsonNode body = resp.getBody();
        List<Place> results = new ArrayList<>();
        if (body == null) return results;

        JsonNode pois = body.path("pois");
        if (pois.isArray()) {
            for (JsonNode p : pois) {
                String id = p.path("id").asText();
                String name = p.path("name").asText("未命名企业");
                String address = p.path("address").asText("");
                String loc = p.path("location").asText("");
                double gcjPLon = Double.NaN, gcjPLat = Double.NaN;
                if (loc.contains(",")) {
                    try {
                        String[] parts = loc.split(",");
                        gcjPLon = Double.parseDouble(parts[0]);
                        gcjPLat = Double.parseDouble(parts[1]);
                    } catch (Exception ignored) {}
                }
                if (Double.isNaN(gcjPLat) || Double.isNaN(gcjPLon)) continue;

                double[] wgs = CoordTransform.gcjToWgs(gcjPLat, gcjPLon);
                double lat = wgs[0];
                double lng = wgs[1];
                double distance = GeoUtils.haversineMeters(wgsLat, wgsLng, lat, lng);

                List<String> categories = new ArrayList<>();
                String type = p.path("type").asText("");
                if (!type.isEmpty()) categories.add(type);
                String typecode = p.path("typecode").asText("");
                if (!typecode.isEmpty()) categories.add("typecode:" + typecode);

                String url = "https://uri.amap.com/marker?position=" + gcjPLon + "," + gcjPLat + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
                results.add(new Place(id, name, address, lat, lng, distance, categories, "amap", url));
            }
        }
        return results;
    }
}