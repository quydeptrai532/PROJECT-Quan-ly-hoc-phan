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
@Table(name = "yeu_cau_ho_tro", indexes = {
        @Index(name = "idx_ycht_sinh_vien_created", columnList = "sinh_vien_id,created_at"),
        @Index(name = "idx_ycht_trang_thai_created", columnList = "trang_thai,created_at"),
        @Index(name = "idx_ycht_ma_phieu", columnList = "ma_phieu", unique = true)
})
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_phieu", nullable = false, unique = true, length = 32, updatable = false)
    private String maPhieu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false, updatable = false)
    private Student sinhVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_tao_user_id", nullable = false, updatable = false)
    private User nguoiTao;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_yeu_cau", nullable = false, length = 40, updatable = false)
    private SupportRequestType loaiYeuCau;

    @Column(name = "tieu_de", nullable = false, length = 200, updatable = false)
    private String tieuDe;

    @Column(name = "noi_dung_goc", nullable = false, length = 4000, updatable = false)
    private String noiDungGoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mon_hoc_id", updatable = false)
    private Subject monHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diem_id", updatable = false)
    private Grade diem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_id", updatable = false)
    private Semester hocKy;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 40)
    private SupportRequestStatus trangThai = SupportRequestStatus.MOI_TAO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_xu_ly_user_id")
    private User nguoiXuLy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaPhieu() {
        return maPhieu;
    }

    public void setMaPhieu(String maPhieu) {
        this.maPhieu = maPhieu;
    }

    public Student getSinhVien() {
        return sinhVien;
    }

    public void setSinhVien(Student sinhVien) {
        this.sinhVien = sinhVien;
    }

    public User getNguoiTao() {
        return nguoiTao;
    }

    public void setNguoiTao(User nguoiTao) {
        this.nguoiTao = nguoiTao;
    }

    public SupportRequestType getLoaiYeuCau() {
        return loaiYeuCau;
    }

    public void setLoaiYeuCau(SupportRequestType loaiYeuCau) {
        this.loaiYeuCau = loaiYeuCau;
    }

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getNoiDungGoc() {
        return noiDungGoc;
    }

    public void setNoiDungGoc(String noiDungGoc) {
        this.noiDungGoc = noiDungGoc;
    }

    public Subject getMonHoc() {
        return monHoc;
    }

    public void setMonHoc(Subject monHoc) {
        this.monHoc = monHoc;
    }

    public Grade getDiem() {
        return diem;
    }

    public void setDiem(Grade diem) {
        this.diem = diem;
    }

    public Semester getHocKy() {
        return hocKy;
    }

    public void setHocKy(Semester hocKy) {
        this.hocKy = hocKy;
    }

    public SupportRequestStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(SupportRequestStatus trangThai) {
        this.trangThai = trangThai;
    }

    public User getNguoiXuLy() {
        return nguoiXuLy;
    }

    public void setNguoiXuLy(User nguoiXuLy) {
        this.nguoiXuLy = nguoiXuLy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
