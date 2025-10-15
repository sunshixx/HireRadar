package com.example.demo.model;

import java.time.LocalDateTime;

/**
 * 投递链接模型。
 * <p>
 * 承载从第三方站点聚合的“职位/投递”链接，便于前端展示与一键跳转。
 * </p>
 */
public class JobLink {
    private String title;
    private String url;
    private String source;
    private String description;
    private LocalDateTime collectedAt;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}