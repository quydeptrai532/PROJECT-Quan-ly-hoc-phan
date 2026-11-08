package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "yeu_cau_ho_tro_tep_dinh_kem", indexes = {
        @Index(name = "idx_ycht_tep_yeu_cau", columnList = "yeu_cau_id,id"),
        @Index(name = "idx_ycht_tep_ten_luu_tru", columnList = "ten_luu_tru", unique = true)
})
public class SupportRequestAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yeu_cau_id", nullable = false, updatable = false)
    private SupportRequest yeuCau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_tai_len_user_id", nullable = false, updatable = false)
    private User nguoiTaiLen;

    @Column(name = "ten_tep_goc", nullable = false, length = 255, updatable = false)
    private String tenTepGoc;

    @Column(name = "ten_luu_tru", nullable = false, unique = true, length = 120, updatable = false)
    private String tenLuuTru;

    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    private String contentType;

    @Column(name = "kich_thuoc", nullable = false, updatable = false)
    private long kichThuoc;

    @Column(name = "checksum_sha256", nullable = false, length = 64, updatable = false)
    private String checksumSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SupportRequest getYeuCau() {
        return yeuCau;
    }

    public void setYeuCau(SupportRequest yeuCau) {
        this.yeuCau = yeuCau;
    }

    public User getNguoiTaiLen() {
        return nguoiTaiLen;
    }

    public void setNguoiTaiLen(User nguoiTaiLen) {
        this.nguoiTaiLen = nguoiTaiLen;
    }

    public String getTenTepGoc() {
        return tenTepGoc;
    }

    public void setTenTepGoc(String tenTepGoc) {
        this.tenTepGoc = tenTepGoc;
    }

    public String getTenLuuTru() {
        return tenLuuTru;
    }

    public void setTenLuuTru(String tenLuuTru) {
        this.tenLuuTru = tenLuuTru;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getKichThuoc() {
        return kichThuoc;
    }

    public void setKichThuoc(long kichThuoc) {
        this.kichThuoc = kichThuoc;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
