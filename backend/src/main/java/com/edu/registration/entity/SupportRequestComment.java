package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "yeu_cau_ho_tro_trao_doi", indexes = {
        @Index(name = "idx_ycht_trao_doi_yeu_cau_created", columnList = "yeu_cau_id,created_at,id")
})
public class SupportRequestComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yeu_cau_id", nullable = false, updatable = false)
    private SupportRequest yeuCau;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_gui_user_id", nullable = false, updatable = false)
    private User nguoiGui;

    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro_nguoi_gui", nullable = false, length = 20, updatable = false)
    private Role vaiTroNguoiGui;

    @Column(name = "ten_hien_thi_nguoi_gui", nullable = false, length = 150, updatable = false)
    private String tenHienThiNguoiGui;

    @Column(name = "noi_dung", nullable = false, length = 2000, updatable = false)
    private String noiDung;

    @Column(name = "cong_khai", nullable = false, updatable = false)
    private boolean congKhai = true;

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

    public User getNguoiGui() {
        return nguoiGui;
    }

    public void setNguoiGui(User nguoiGui) {
        this.nguoiGui = nguoiGui;
    }

    public Role getVaiTroNguoiGui() {
        return vaiTroNguoiGui;
    }

    public void setVaiTroNguoiGui(Role vaiTroNguoiGui) {
        this.vaiTroNguoiGui = vaiTroNguoiGui;
    }

    public String getTenHienThiNguoiGui() {
        return tenHienThiNguoiGui;
    }

    public void setTenHienThiNguoiGui(String tenHienThiNguoiGui) {
        this.tenHienThiNguoiGui = tenHienThiNguoiGui;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public boolean isCongKhai() {
        return congKhai;
    }

    public void setCongKhai(boolean congKhai) {
        this.congKhai = congKhai;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
