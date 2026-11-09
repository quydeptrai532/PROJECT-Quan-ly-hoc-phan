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
@Table(name = "yeu_cau_ho_tro_lich_su_trang_thai", indexes = {
        @Index(name = "idx_ycht_lich_su_yeu_cau_thoi_diem", columnList = "yeu_cau_id,thoi_diem,id")
})
public class SupportRequestStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "yeu_cau_id", nullable = false, updatable = false)
    private SupportRequest yeuCau;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thay_doi", nullable = false, length = 40, updatable = false)
    private SupportRequestChangeType loaiThayDoi = SupportRequestChangeType.TRANG_THAI;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_cu", length = 40, updatable = false)
    private SupportRequestStatus trangThaiCu;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_moi", nullable = false, length = 40, updatable = false)
    private SupportRequestStatus trangThaiMoi;

    @Column(name = "gia_tri_cu", length = 500, updatable = false)
    private String giaTriCu;

    @Column(name = "gia_tri_moi", length = 500, updatable = false)
    private String giaTriMoi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_thuc_hien_user_id", nullable = false, updatable = false)
    private User nguoiThucHien;

    @Column(name = "ten_nguoi_thuc_hien", nullable = false, length = 150, updatable = false)
    private String tenNguoiThucHien;

    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro_nguoi_thuc_hien", nullable = false, length = 20, updatable = false)
    private Role vaiTroNguoiThucHien;

    @Column(name = "ghi_chu", length = 500, updatable = false)
    private String ghiChu;

    @Column(name = "thoi_diem", nullable = false, updatable = false)
    private LocalDateTime thoiDiem = LocalDateTime.now();

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

    public SupportRequestChangeType getLoaiThayDoi() {
        return loaiThayDoi;
    }

    public void setLoaiThayDoi(SupportRequestChangeType loaiThayDoi) {
        this.loaiThayDoi = loaiThayDoi;
    }

    public SupportRequestStatus getTrangThaiCu() {
        return trangThaiCu;
    }

    public void setTrangThaiCu(SupportRequestStatus trangThaiCu) {
        this.trangThaiCu = trangThaiCu;
    }

    public SupportRequestStatus getTrangThaiMoi() {
        return trangThaiMoi;
    }

    public void setTrangThaiMoi(SupportRequestStatus trangThaiMoi) {
        this.trangThaiMoi = trangThaiMoi;
    }

    public String getGiaTriCu() {
        return giaTriCu;
    }

    public void setGiaTriCu(String giaTriCu) {
        this.giaTriCu = giaTriCu;
    }

    public String getGiaTriMoi() {
        return giaTriMoi;
    }

    public void setGiaTriMoi(String giaTriMoi) {
        this.giaTriMoi = giaTriMoi;
    }

    public User getNguoiThucHien() {
        return nguoiThucHien;
    }

    public void setNguoiThucHien(User nguoiThucHien) {
        this.nguoiThucHien = nguoiThucHien;
    }

    public String getTenNguoiThucHien() {
        return tenNguoiThucHien;
    }

    public void setTenNguoiThucHien(String tenNguoiThucHien) {
        this.tenNguoiThucHien = tenNguoiThucHien;
    }

    public Role getVaiTroNguoiThucHien() {
        return vaiTroNguoiThucHien;
    }

    public void setVaiTroNguoiThucHien(Role vaiTroNguoiThucHien) {
        this.vaiTroNguoiThucHien = vaiTroNguoiThucHien;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public LocalDateTime getThoiDiem() {
        return thoiDiem;
    }

    public void setThoiDiem(LocalDateTime thoiDiem) {
        this.thoiDiem = thoiDiem;
    }
}
