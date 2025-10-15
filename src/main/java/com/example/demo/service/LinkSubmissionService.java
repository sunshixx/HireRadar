package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.demo.entity.SubmittedLinkEntity;
import com.example.demo.mapper.SubmittedLinkMapper;
import com.example.demo.model.JobLink;
import com.example.demo.model.SubmittedLink;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 链接提交与审核服务（DB 版）。
 * <p>
 * 使用 MyBatis-Plus 持久化，提供提交、审核与查询；并将审核通过的链接转换为 JobLink。
 * </p>
 */
@Service
public class LinkSubmissionService {
    private final SubmittedLinkMapper mapper;

    public LinkSubmissionService(SubmittedLinkMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 提交一个链接，初始状态为 PENDING。
     *
     * @param link 链接
     * @return 带 ID 的链接
     */
    public SubmittedLink submit(SubmittedLink link) {
        Objects.requireNonNull(link, "link");
        SubmittedLinkEntity e = toEntity(link);
        e.setStatus("PENDING");
        e.setValid(Boolean.TRUE);
        e.setFailureCount(0);
        e.setSubmittedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        mapper.insert(e);
        return toModel(e);
    }

    /** 审核通过指定链接。 */
    public boolean approve(String id) {
        return updateStatus(id, "APPROVED");
    }

    /** 审核拒绝指定链接。 */
    public boolean reject(String id) {
        return updateStatus(id, "REJECTED");
    }

    /** 根据公司与状态查询链接列表。 */
    public List<SubmittedLink> list(String companyName, String status) {
        LambdaQueryWrapper<SubmittedLinkEntity> qw = new LambdaQueryWrapper<>();
        if (companyName != null && !companyName.isBlank()) {
            qw.eq(SubmittedLinkEntity::getCompanyName, companyName);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(SubmittedLinkEntity::getStatus, status);
        }
        List<SubmittedLinkEntity> list = mapper.selectList(qw);
        return list.stream().map(this::toModel).collect(Collectors.toList());
    }

    /** 列出指定公司审核通过的“投递链接(APPLY)”并转换为 JobLink。 */
    public List<JobLink> listApprovedApplyLinks(String companyName) {
        LambdaQueryWrapper<SubmittedLinkEntity> qw = new LambdaQueryWrapper<>()
                .eq(SubmittedLinkEntity::getCompanyName, companyName)
                .eq(SubmittedLinkEntity::getStatus, "APPROVED")
                .eq(SubmittedLinkEntity::getType, "APPLY")
                .eq(SubmittedLinkEntity::getValid, true)
                .isNull(SubmittedLinkEntity::getExpireAt);
        return mapper.selectList(qw).stream().map(this::toJobLinkApply).collect(Collectors.toList());
    }

    /** 列出指定公司审核通过的“公告链接(ANNOUNCEMENT)”并转换为 JobLink。 */
    public List<JobLink> listApprovedAnnouncementLinks(String companyName) {
        LambdaQueryWrapper<SubmittedLinkEntity> qw = new LambdaQueryWrapper<SubmittedLinkEntity>()
                .eq(SubmittedLinkEntity::getCompanyName, companyName)
                .eq(SubmittedLinkEntity::getStatus, "APPROVED")
                .eq(SubmittedLinkEntity::getType, "ANNOUNCEMENT")
                .eq(SubmittedLinkEntity::getValid, true)
                .isNull(SubmittedLinkEntity::getExpireAt);
        return mapper.selectList(qw).stream().map(this::toJobLinkAnnouncement).collect(Collectors.toList());
    }

    /** 通过 ID 查找链接。 */
    public Optional<SubmittedLink> findById(String id) {
        SubmittedLinkEntity e = mapper.selectById(id);
        return Optional.ofNullable(e).map(this::toModel);
    }

    /** 更新状态通用方法。 */
    private boolean updateStatus(String id, String to) {
        LambdaUpdateWrapper<SubmittedLinkEntity> uw = new LambdaUpdateWrapper<>()
                .eq(SubmittedLinkEntity::getId, id)
                .set(SubmittedLinkEntity::getStatus, to)
                .set(SubmittedLinkEntity::getUpdatedAt, LocalDateTime.now());
        return mapper.update(null, uw) > 0;
    }

    /** Entity -> Model 转换。 */
    private SubmittedLink toModel(SubmittedLinkEntity e) {
        SubmittedLink m = new SubmittedLink();
        m.setId(e.getId());
        m.setCompanyName(e.getCompanyName());
        m.setTitle(e.getTitle());
        m.setUrl(e.getUrl());
        m.setType(e.getType());
        m.setSource(e.getSource());
        m.setStatus(e.getStatus());
        m.setRemarks(e.getRemarks());
        m.setSubmittedAt(e.getSubmittedAt());
        m.setUpdatedAt(e.getUpdatedAt());
        return m;
    }

    /** Model -> Entity 转换。 */
    private SubmittedLinkEntity toEntity(SubmittedLink m) {
        SubmittedLinkEntity e = new SubmittedLinkEntity();
        e.setId(m.getId());
        e.setCompanyName(m.getCompanyName());
        e.setTitle(m.getTitle());
        e.setUrl(m.getUrl());
        e.setType(m.getType());
        e.setSource(m.getSource());
        e.setStatus(m.getStatus());
        e.setRemarks(m.getRemarks());
        e.setSubmittedAt(m.getSubmittedAt());
        e.setUpdatedAt(m.getUpdatedAt());
        return e;
    }

    /** Entity -> JobLink（投递）。 */
    private JobLink toJobLinkApply(SubmittedLinkEntity e) {
        JobLink jl = new JobLink();
        jl.setTitle(Optional.ofNullable(e.getTitle()).orElse("投递入口"));
        jl.setUrl(e.getUrl());
        jl.setSource(Optional.ofNullable(e.getSource()).orElse("submitted"));
        jl.setDescription("来源：提交审核");
        jl.setCollectedAt(Optional.ofNullable(e.getUpdatedAt()).orElse(LocalDateTime.now()));
        return jl;
    }

    /** Entity -> JobLink（公告）。 */
    private JobLink toJobLinkAnnouncement(SubmittedLinkEntity e) {
        JobLink jl = new JobLink();
        jl.setTitle(Optional.ofNullable(e.getTitle()).orElse("公告"));
        jl.setUrl(e.getUrl());
        jl.setSource(Optional.ofNullable(e.getSource()).orElse("submitted"));
        jl.setDescription("来源：提交审核");
        jl.setCollectedAt(Optional.ofNullable(e.getUpdatedAt()).orElse(LocalDateTime.now()));
        return jl;
    }
}