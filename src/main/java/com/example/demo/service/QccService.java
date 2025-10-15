package com.example.demo.service;

import com.example.demo.model.CompanyDetail;
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
import java.time.LocalDate;
import java.util.Optional;

/**
 * 企查查适配服务。
 * <p>
 * 为“按需丰富”提供公司工商信息查询能力。为了避免限流与高成本，
 * 仅在用户点击某家公司时触发查询。未配置密钥时返回空结果。
 * </p>
 */
@Service
public class QccService {
    /** 企查查查询接口（示意）：具体路径以签约文档为准。 */
    private static final String QCC_SEARCH_URL = "https://api.qichacha.com/SearchCompany";

    @Value("${qcc.api.key:}")
    private String apiKey;

    @Value("${qcc.api.token:}")
    private String apiToken;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 判断服务是否已配置。
     *
     * @return true 表示已配置密钥，可调用企查查接口；否则返回 false
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && apiToken != null && !apiToken.isBlank();
    }

    /**
     * 根据公司名称与地址进行工商详情查询（按需丰富）。
     * <p>
     * 说明：企查查接口通常以名称精确查询为主，地址用于后续匹配；
     * 返回空 Optional 表示未配置或未命中。
     * </p>
     *
     * @param name    公司名称
     * @param address 公司地址（用于匹配辅助）
     * @return 命中时返回公司详情；否则返回空 Optional
     */
    public Optional<CompanyDetail> enrichByName(String name, String address) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            String qs = "key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) +
                    "&token=" + URLEncoder.encode(apiToken, StandardCharsets.UTF_8) +
                    "&keyword=" + URLEncoder.encode(name, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, "NearbyCompaniesDemo/1.0");
            HttpEntity<String> entity = new HttpEntity<>(qs, headers);

            ResponseEntity<JsonNode> resp = restTemplate.exchange(QCC_SEARCH_URL, HttpMethod.POST, entity, JsonNode.class);
            JsonNode body = resp.getBody();
            if (body == null) {
                return Optional.empty();
            }

            // 解析示意：具体字段名以企查查文档为准，这里做健壮性兜底
            JsonNode data = body.path("Result");
            if (data.isArray() && data.size() > 0) {
                JsonNode first = data.get(0);
                CompanyDetail detail = new CompanyDetail();
                detail.setSource("qcc");
                detail.setName(first.path("Name").asText(name));
                detail.setUnifiedSocialCreditCode(first.path("CreditCode").asText(""));
                detail.setLegalPerson(first.path("OperName").asText(""));
                detail.setRegisteredCapital(first.path("RegistCapi").asText(""));
                String estDateStr = first.path("StartDate").asText("");
                if (!estDateStr.isEmpty()) {
                    try { detail.setEstablishmentDate(LocalDate.parse(estDateStr)); } catch (Exception ignore) {}
                }
                detail.setAddress(first.path("Address").asText(address));
                detail.setBusinessScope(first.path("Scope").asText(""));
                detail.setPhone(first.path("Phone").asText(""));
                detail.setEmail(first.path("Email").asText(""));
                detail.setWebsite(first.path("WebSite").asText(""));
                return Optional.of(detail);
            }
            return Optional.empty();
        } catch (Exception ex) {
            // 接口不可用或限流时返回空，避免影响主流程
            return Optional.empty();
        }
    }
}