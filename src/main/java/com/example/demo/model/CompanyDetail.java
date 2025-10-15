package com.example.demo.model;

import java.time.LocalDate;

/**
 * 公司工商详情模型。
 * <p>
 * 用于承载企查查等第三方数据源返回的公司基础信息，
 * 在点击列表项进行“按需丰富”时返回给前端展示。
 * </p>
 */
public class CompanyDetail {
    private String name;
    private String unifiedSocialCreditCode;
    private String legalPerson;
    private String registeredCapital;
    private LocalDate establishmentDate;
    private String address;
    private String businessScope;
    private String phone;
    private String email;
    private String website;
    private String source;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnifiedSocialCreditCode() {
        return unifiedSocialCreditCode;
    }

    public void setUnifiedSocialCreditCode(String unifiedSocialCreditCode) {
        this.unifiedSocialCreditCode = unifiedSocialCreditCode;
    }

    public String getLegalPerson() {
        return legalPerson;
    }

    public void setLegalPerson(String legalPerson) {
        this.legalPerson = legalPerson;
    }

    public String getRegisteredCapital() {
        return registeredCapital;
    }

    public void setRegisteredCapital(String registeredCapital) {
        this.registeredCapital = registeredCapital;
    }

    public LocalDate getEstablishmentDate() {
        return establishmentDate;
    }

    public void setEstablishmentDate(LocalDate establishmentDate) {
        this.establishmentDate = establishmentDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBusinessScope() {
        return businessScope;
    }

    public void setBusinessScope(String businessScope) {
        this.businessScope = businessScope;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}