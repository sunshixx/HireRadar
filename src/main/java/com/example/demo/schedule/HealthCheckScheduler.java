package com.example.demo.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.entity.SubmittedLinkEntity;
import com.example.demo.mapper.SubmittedLinkMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 链接健康检查与过期清理调度。
 * <p>
 * 每日检查审核通过的链接是否有效（状态码 200-399 认为有效），
 * 连续失败超过阈值或到达过期时间则标记为 EXPIRED。
 * </p>
 */
@Component
public class HealthCheckScheduler {

    private final SubmittedLinkMapper mapper;

    public HealthCheckScheduler(SubmittedLinkMapper mapper) {
        this.mapper = mapper;
    }

    /** 每日 02:30 进行健康检查与过期清理。 */
    @Scheduled(cron = "0 30 2 * * ?")
    public void checkAndExpireDaily() {
        int failThreshold = 3;
        LocalDateTime now = LocalDateTime.now();
        // 选取 APPROVED 且未过期的记录
        LambdaQueryWrapper<SubmittedLinkEntity> qw = new LambdaQueryWrapper<SubmittedLinkEntity>()
                .eq(SubmittedLinkEntity::getStatus, "APPROVED")
                .isNull(SubmittedLinkEntity::getExpireAt);
        List<SubmittedLinkEntity> list = mapper.selectList(qw);
        for (SubmittedLinkEntity e : list) {
            boolean ok = isUrlAlive(e.getUrl());
            Integer fail = e.getFailureCount() == null ? 0 : e.getFailureCount();
            LambdaUpdateWrapper<SubmittedLinkEntity> uw = new LambdaUpdateWrapper<SubmittedLinkEntity>()
                    .eq(SubmittedLinkEntity::getId, e.getId())
                    .set(SubmittedLinkEntity::getLastCheckedAt, now)
                    .set(SubmittedLinkEntity::getValid, ok)
                    .set(SubmittedLinkEntity::getUpdatedAt, now);
            if (!ok) {
                fail = fail + 1;
                uw.set(SubmittedLinkEntity::getFailureCount, fail);
                if (fail >= failThreshold) {
                    uw.set(SubmittedLinkEntity::getStatus, "EXPIRED")
                      .set(SubmittedLinkEntity::getExpireAt, now);
                }
            } else {
                // 成功则清零失败计数
                uw.set(SubmittedLinkEntity::getFailureCount, 0);
            }
            mapper.update(null, uw);
        }
    }

    /**
     * 链接健康探测：优先 HEAD，不支持则 GET；状态码 200-399 认为有效。
     *
     * @param url 链接
     * @return 是否可用
     */
    private boolean isUrlAlive(String url) {
        try {
            // HEAD 尝试
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            int code = con.getResponseCode();
            if (code >= 200 && code < 400) {
                return true;
            }
        } catch (Exception ignore) {}
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(true);
            con.setConnectTimeout(7000);
            con.setReadTimeout(7000);
            int code = con.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }
}