package com.example.demo.schedule;

import com.example.demo.model.JobLink;
import com.example.demo.service.JobLinkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 投递链接定时刷新任务。
 * <p>
 * 每日按照配置的公司名列表执行一次聚合刷新，结果缓存于内存供接口快速返回。
 * </p>
 */
@Component
public class JobLinkScheduler {

    private final JobLinkService jobLinkService;

    @Value("${jobs.scheduler.companyNames:腾讯,阿里巴巴,字节跳动,美团}")
    private String companyNames;

    public JobLinkScheduler(JobLinkService jobLinkService) {
        this.jobLinkService = jobLinkService;
    }

    /**
     * 每日凌晨 3 点执行一次（可调整 cron）。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void refreshDaily() {
        List<String> names = new ArrayList<>(Arrays.asList(companyNames.split(",")));
        for (String name : names) {
            List<JobLink> links = jobLinkService.searchLinks(name.trim());
            // 演示：只刷新，将结果留在内存 cache 中，接口直接返回
        }
    }
}