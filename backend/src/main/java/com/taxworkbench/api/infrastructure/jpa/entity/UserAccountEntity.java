package com.taxworkbench.api.infrastructure.jpa.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_USER", indexes = {
        @Index(name = "IDX_TB_USER_ID", columnList = "ID", unique = true)
})
public class UserAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SEQ")
    private Long seq;

    @Column(name = "ID", nullable = false, length = 20)
    private String id;

    @Column(name = "NAME", nullable = false, length = 20)
    private String name;

    @Column(name = "PSWD", nullable = false, length = 255)
    private String pswd;

    @Column(name = "EMAIL", nullable = false, length = 20)
    private String email;

    @Column(name = "MOBILE_NO", nullable = false, length = 20)
    private String mobileNo;

    @Column(name = "USE_YN", nullable = false, length = 1)
    private String useYn;

    @Column(name = "CRT_BY", nullable = false, length = 20)
    private String crtBy;

    @Column(name = "CRT_DT", nullable = false)
    private LocalDateTime crtDt;

    @Column(name = "CRT_IP", nullable = false, length = 20)
    private String crtIp;

    @Column(name = "AMN_BY", length = 20)
    private String amnBy;

    @Column(name = "AMN_DT")
    private LocalDateTime amnDt;

    @Column(name = "AMN_IP", length = 20)
    private String amnIp;

    public Long getSeq() { return seq; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPswd() { return pswd; }
    public void setPswd(String pswd) { this.pswd = pswd; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
    public String getCrtBy() { return crtBy; }
    public void setCrtBy(String crtBy) { this.crtBy = crtBy; }
    public LocalDateTime getCrtDt() { return crtDt; }
    public void setCrtDt(LocalDateTime crtDt) { this.crtDt = crtDt; }
    public String getCrtIp() { return crtIp; }
    public void setCrtIp(String crtIp) { this.crtIp = crtIp; }
    public String getAmnBy() { return amnBy; }
    public void setAmnBy(String amnBy) { this.amnBy = amnBy; }
    public LocalDateTime getAmnDt() { return amnDt; }
    public void setAmnDt(LocalDateTime amnDt) { this.amnDt = amnDt; }
    public String getAmnIp() { return amnIp; }
    public void setAmnIp(String amnIp) { this.amnIp = amnIp; }
}
