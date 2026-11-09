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
@Table(name = "lich_su_phuc_khao_diem", indexes = {
        @Index(name = "idx_ls_phuc_khao_ho_so", columnList = "ho_so_id,thoi_diem")
})
public class GradeAppealHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ho_so_id", nullable = false, updatable = false)
    private GradeAppeal hoSo;

    @Column(name = "hanh_dong", nullable = false, length = 64, updatable = false)
    private String hanhDong;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", length = 32, updatable = false)
    private GradeAppealStatus trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 32, updatable = false)
    private GradeAppealStatus trangThaiSau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_user_id", updatable = false)
    private User nguoiThucHien;

    @Column(name = "nguoi_thuc_hien_username", length = 100, updatable = false)
    private String nguoiThucHienUsername;

    @Column(name = "nguoi_thuc_hien_ho_ten", length = 150, updatable = false)
    private String nguoiThucHienHoTen;

    @Enumerated(EnumType.STRING)
    @Column(name = "nguoi_thuc_hien_role", length = 32, updatable = false)
    private Role nguoiThucHienRole;

    @Column(name = "ly_do", length = 2000, updatable = false)
    private String lyDo;

    @Column(name = "diem_qua_trinh_moi", updatable = false)
    private Double diemQuaTrinhMoi;

    @Column(name = "diem_cuoi_ky_moi", updatable = false)
    private Double diemCuoiKyMoi;

    @Column(name = "diem_tong_ket_moi", updatable = false)
    private Double diemTongKetMoi;

    @Column(name = "thoi_diem", nullable = false, updatable = false)
    private LocalDateTime thoiDiem = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GradeAppeal getHoSo() {
        return hoSo;
    }

    public void setHoSo(GradeAppeal hoSo) {
        this.hoSo = hoSo;
    }

    public String getHanhDong() {
        return hanhDong;
    }

    public void setHanhDong(String hanhDong) {
        this.hanhDong = hanhDong;
    }

    public GradeAppealStatus getTrangThaiTruoc() {
        return trangThaiTruoc;
    }

    public void setTrangThaiTruoc(GradeAppealStatus trangThaiTruoc) {
        this.trangThaiTruoc = trangThaiTruoc;
    }

    public GradeAppealStatus getTrangThaiSau() {
        return trangThaiSau;
    }

    public void setTrangThaiSau(GradeAppealStatus trangThaiSau) {
        this.trangThaiSau = trangThaiSau;
    }

    public User getNguoiThucHien() {
        return nguoiThucHien;
    }

    public void setNguoiThucHien(User nguoiThucHien) {
        this.nguoiThucHien = nguoiThucHien;
    }

    public String getNguoiThucHienUsername() {
        return nguoiThucHienUsername;
    }

    public void setNguoiThucHienUsername(String nguoiThucHienUsername) {
        this.nguoiThucHienUsername = nguoiThucHienUsername;
    }

    public String getNguoiThucHienHoTen() {
        return nguoiThucHienHoTen;
    }

    public void setNguoiThucHienHoTen(String nguoiThucHienHoTen) {
        this.nguoiThucHienHoTen = nguoiThucHienHoTen;
    }

    public Role getNguoiThucHienRole() {
        return nguoiThucHienRole;
    }

    public void setNguoiThucHienRole(Role nguoiThucHienRole) {
        this.nguoiThucHienRole = nguoiThucHienRole;
    }

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String lyDo) {
        this.lyDo = lyDo;
    }

    public Double getDiemQuaTrinhMoi() {
        return diemQuaTrinhMoi;
    }

    public void setDiemQuaTrinhMoi(Double diemQuaTrinhMoi) {
        this.diemQuaTrinhMoi = diemQuaTrinhMoi;
    }

    public Double getDiemCuoiKyMoi() {
        return diemCuoiKyMoi;
    }

    public void setDiemCuoiKyMoi(Double diemCuoiKyMoi) {
        this.diemCuoiKyMoi = diemCuoiKyMoi;
    }

    public Double getDiemTongKetMoi() {
        return diemTongKetMoi;
    }

    public void setDiemTongKetMoi(Double diemTongKetMoi) {
        this.diemTongKetMoi = diemTongKetMoi;
    }

    public LocalDateTime getThoiDiem() {
        return thoiDiem;
    }

    public void setThoiDiem(LocalDateTime thoiDiem) {
        this.thoiDiem = thoiDiem;
    }
}
