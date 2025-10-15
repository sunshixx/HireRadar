package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 提交链接实体（持久化）。
 */
@TableName("submitted_links")
public class SubmittedLinkEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String companyName;
    private String title;
    private String url;
    private String type; // APPLY / ANNOUNCEMENT
    private String source; // user/company/admin
    private String status; // PENDING/APPROVED/REJECTED/EXPIRED
    private String remarks;
    private Boolean valid; // 健康检查结果
    private Integer failureCount;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastCheckedAt;
    private LocalDateTime expireAt;

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
    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }
    public Integer getFailureCount() { return failureCount; }
    public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
}