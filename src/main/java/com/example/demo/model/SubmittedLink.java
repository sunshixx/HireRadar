package com.example.demo.model;

import java.time.LocalDateTime;

/**
 * 提交的外部链接（投递/公告）。
 * <p>
 * 该模型用于承载企业或用户提交的“投递入口链接”或“公告链接”，
 * 后台可进行审核，审核通过后参与聚合并在前端展示。
 * </p>
 */
public class SubmittedLink {
    private String id;
    private String companyName;
    private String title;
    private String url;
    /** 链接类型：APPLY 或 ANNOUNCEMENT */
    private String type;
    /** 来源：user/company/admin */
    private String source;
    /** 状态：PENDING/APPROVED/REJECTED */
    private String status;
    private String remarks;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}