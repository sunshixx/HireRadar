package com.example.demo.controller;

import com.example.demo.model.NearbyResponse;
import com.example.demo.model.Place;
import com.example.demo.model.CompanyDetail;
import com.example.demo.model.JobLink;
import com.example.demo.service.AmapService;
import com.example.demo.service.OverpassService;
import com.example.demo.service.QccService;
import com.example.demo.service.JobLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
public class CompanyController {
    private final OverpassService overpassService;
    private final AmapService amapService;
    private final QccService qccService;
    private final JobLinkService jobLinkService;

    public CompanyController(OverpassService overpassService, AmapService amapService, QccService qccService, JobLinkService jobLinkService) {
        this.overpassService = overpassService;
        this.amapService = amapService;
        this.qccService = qccService;
        this.jobLinkService = jobLinkService;
    }

    /**
     * 附近公司检索接口。
     * <p>
     * 根据坐标与半径，从指定数据源（高德/OSM）检索附近公司并按距离排序。
     * 若配置了高德密钥且未指定 source，则默认使用高德；否则使用 OSM。
     * </p>
     *
     * @param lat     纬度（WGS-84）
     * @param lng     经度（WGS-84）
     * @param radius  半径（米），默认 1500
     * @param keyword 关键词（可选）
     * @param source  数据源：amap 或 osm（可选）
     * @return 标准化结果列表与元信息
     */
    @GetMapping("/api/companies/nearby")
    public ResponseEntity<NearbyResponse> nearby(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "radius", required = false, defaultValue = "1500") int radius,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "source", required = false) String source
    ) {
        String provider;
        List<Place> items;
        if ("amap".equalsIgnoreCase(String.valueOf(source)) && amapService.isConfigured()) {
            provider = "amap";
            items = amapService.searchNearby(lat, lng, radius, keyword);
        } else if ("osm".equalsIgnoreCase(String.valueOf(source))) {
            provider = "osm-overpass";
            items = overpassService.searchNearby(lat, lng, radius, keyword);
        } else if (amapService.isConfigured()) {
            provider = "amap";
            items = amapService.searchNearby(lat, lng, radius, keyword);
        } else {
            provider = "osm-overpass";
            items = overpassService.searchNearby(lat, lng, radius, keyword);
        }

        items.sort(Comparator.comparingDouble(Place::getDistance));
        NearbyResponse.Meta meta = new NearbyResponse.Meta(lat, lng, radius, provider, keyword);
        NearbyResponse response = new NearbyResponse(items, meta);
        return ResponseEntity.ok(response);
    }

    /**
     * 公司工商详情按需丰富接口。
     * <p>
     * 传入公司名称与地址，使用企查查进行工商信息查询。未配置密钥或未命中时返回 204。
     * </p>
     *
     * @param name    公司名称
     * @param address 公司地址（可选）
     * @return 命中时返回公司详情；否则 204 No Content
     */
    @GetMapping("/api/companies/enrich")
    public ResponseEntity<CompanyDetail> enrich(
            @RequestParam("name") String name,
            @RequestParam(value = "address", required = false) String address
    ) {
        return qccService.enrichByName(name, address)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * 公司投递链接聚合接口（按需丰富）。
     * <p>
     * 根据公司名称聚合“职位/投递”链接；仅在用户点击需要时调用，避免批量抓取导致限流与成本问题。
     * </p>
     *
     * @param name 公司名称
     * @return 投递链接列表；无数据返回空列表
     */
    @GetMapping("/api/companies/jobs")
    public ResponseEntity<java.util.List<JobLink>> jobs(@RequestParam("name") String name) {
        java.util.List<JobLink> list = jobLinkService.searchLinks(name);
        return ResponseEntity.ok(list);
    }
}