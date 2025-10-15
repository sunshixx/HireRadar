package com.example.demo.service;

import com.example.demo.model.JobLink;

import java.util.List;

/**
 * 投递链接爬虫接口。
 * <p>
 * 不同站点（如：牛客、OfferShow、公司官网）实现该接口，
 * 通过公司名称进行检索并返回投递链接列表。
 * </p>
 */
public interface JobCrawler {

    /**
     * 按公司名称检索投递链接。
     *
     * @param companyName 公司名称
     * @param limit       返回条数上限
     * @return 投递链接列表（不要求排序）
     */
    List<JobLink> crawlByCompanyName(String companyName, int limit);

    /**
     * 返回数据源标识（站点名）。
     *
     * @return 站点名，例如 nowcoder、offershow、career
     */
    String getSourceName();
}