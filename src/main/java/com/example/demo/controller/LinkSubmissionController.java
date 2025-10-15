package com.example.demo.controller;

import com.example.demo.model.JobLink;
import com.example.demo.model.SubmittedLink;
import com.example.demo.service.LinkSubmissionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 链接提交与审核控制器。
 * <p>
 * 提供提交接口与审核接口；公告链接返回接口用于前端展示“公告”入口。
 * </p>
 */
@RestController
public class LinkSubmissionController {
    private final LinkSubmissionService submissionService;
    @Value("${admin.token:}")
    private String adminToken;

    public LinkSubmissionController(LinkSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * 提交链接（投递/公告）。
     *
     * @param body JSON：companyName、title、url、type(APPLY/ANNOUNCEMENT)、source(optional)、remarks(optional)
     * @return 创建后的链接（状态 PENDING）
     */
    @PostMapping("/api/links/submit")
    public ResponseEntity<SubmittedLink> submit(@RequestBody Map<String, String> body) {
        SubmittedLink l = new SubmittedLink();
        l.setCompanyName(body.getOrDefault("companyName", ""));
        l.setTitle(body.getOrDefault("title", ""));
        l.setUrl(body.getOrDefault("url", ""));
        l.setType(body.getOrDefault("type", "APPLY"));
        l.setSource(body.getOrDefault("source", "user"));
        l.setRemarks(body.getOrDefault("remarks", ""));
        if (!StringUtils.hasText(l.getCompanyName()) || !StringUtils.hasText(l.getUrl())) {
            return ResponseEntity.badRequest().build();
        }
        SubmittedLink saved = submissionService.submit(l);
        return ResponseEntity.ok(saved);
    }

    /**
     * 审核列表查询。
     *
     * @param company 公司名（可选）
     * @param status  状态（PENDING/APPROVED/REJECTED，可选）
     * @return 链接列表
     */
    @GetMapping("/api/links/moderate")
    public ResponseEntity<List<SubmittedLink>> listModerate(
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "status", required = false, defaultValue = "PENDING") String status,
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") int pageSize) {
        if (!isAdminAuthorized(token)) return ResponseEntity.status(401).build();
        List<SubmittedLink> list = submissionService.list(company, status);
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(list.size(), from + pageSize);
        if (from >= to) return ResponseEntity.ok(List.of());
        list = list.subList(from, to);
        return ResponseEntity.ok(list);
    }

    /**
     * 审核操作：通过或拒绝。
     *
     * @param id     链接 ID
     * @param action 操作：approve 或 reject
     * @return 操作结果
     */
    @PutMapping("/api/links/moderate/{id}")
    public ResponseEntity<Void> moderate(@PathVariable("id") String id,
                                         @RequestParam("action") String action,
                                         @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!isAdminAuthorized(token)) return ResponseEntity.status(401).build();
        boolean ok;
        if ("approve".equalsIgnoreCase(action)) {
            ok = submissionService.approve(id);
        } else if ("reject".equalsIgnoreCase(action)) {
            ok = submissionService.reject(id);
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * 公告链接查询（审核通过）。
     *
     * @param name 公司名称
     * @return 公告链接列表（使用 JobLink 结构复用）
     */
    @GetMapping("/api/companies/announcements")
    public ResponseEntity<List<JobLink>> announcements(@RequestParam("name") String name) {
        List<JobLink> list = submissionService.listApprovedAnnouncementLinks(name);
        return ResponseEntity.ok(list);
    }

    /**
     * 导出审核通过的链接（CSV/JSON）。
     *
     * @param format  csv 或 json
     * @param company 公司名（可选）
     * @param status  状态（默认 APPROVED）
     */
    @GetMapping("/api/links/export")
    public ResponseEntity<?> export(
            @RequestParam(value = "format", required = false, defaultValue = "csv") String format,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "status", required = false, defaultValue = "APPROVED") String status,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!isAdminAuthorized(token)) return ResponseEntity.status(401).build();
        List<SubmittedLink> list = submissionService.list(company, status);
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(list);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("companyName,type,title,url,source,remarks\n");
        for (SubmittedLink l : list) {
            sb.append(escapeCsv(l.getCompanyName())).append(',')
              .append(escapeCsv(l.getType())).append(',')
              .append(escapeCsv(l.getTitle())).append(',')
              .append(escapeCsv(l.getUrl())).append(',')
              .append(escapeCsv(l.getSource())).append(',')
              .append(escapeCsv(l.getRemarks())).append('\n');
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=links.csv");
        return ResponseEntity.ok().headers(headers).body(sb.toString());
    }

    private boolean isAdminAuthorized(String token) {
        return adminToken == null || adminToken.isBlank() || (token != null && token.equals(adminToken));
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (t.contains(",") || t.contains("\n") || t.contains("\r")) {
            return "\"" + t + "\"";
        }
        return t;
    }
}