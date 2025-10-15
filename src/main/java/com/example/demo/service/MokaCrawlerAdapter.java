package com.example.demo.service;

import com.example.demo.model.JobLink;

import java.util.List;

/**
 * Moka 爬虫适配器。
 * <p>
 * 通过 {@link MokaService} 提供的官方 API 或搜索链接，
 * 将结果适配为统一的 JobLink 列表供聚合服务使用。
 * </p>
 */
public class MokaCrawlerAdapter implements JobCrawler {
    private final MokaService mokaService;

    /**
     * 构造函数。
     *
     * @param mokaService Moka 服务实例
     */
    public MokaCrawlerAdapter(MokaService mokaService) {
        this.mokaService = mokaService;
    }

    /**
     * 调用 Moka 服务按公司名检索投递链接。
     *
     * @param companyName 公司名称
     * @param limit       返回条数上限
     * @return 投递链接列表
     */
    @Override
    public List<JobLink> crawlByCompanyName(String companyName, int limit) {
        return mokaService.searchByCompanyName(companyName, limit);
    }

    /**
     * 返回数据源标识。
     *
     * @return 字符串 "moka"
     */
    @Override
    public String getSourceName() {
        return "moka";
    }
}