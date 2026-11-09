package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "phong_hoc")
public class Classroom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma", nullable = false, unique = true, length = 30)
    private String ma;

    @Column(name = "ten", nullable = false, length = 150)
    private String ten;

    @Column(name = "toa_nha", length = 50)
    private String toaNha;

    @Column(name = "tang")
    private Integer tang;

    @Column(name = "vi_tri", length = 150)
    private String viTri;

    @Column(name = "suc_chua", nullable = false)
    private int sucChua;

    @Column(name = "loai_phong", nullable = false, length = 50)
    private String loaiPhong = "LY_THUYET";

    @Column(name = "thiet_bi", length = 500)
    private String thietBi;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private ClassroomStatus trangThai = ClassroomStatus.HOAT_DONG;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_cap_nhat_username", length = 100)
    private String nguoiCapNhatUsername;

    public Classroom() {
    }

    public Classroom(String ma, String ten, String toaNha, Integer tang, String viTri,
                     int sucChua, String loaiPhong, String thietBi, ClassroomStatus trangThai, String ghiChu) {
        this.ma = ma;
        this.ten = ten;
        this.toaNha = toaNha;
        this.tang = tang;
        this.viTri = viTri;
        this.sucChua = sucChua;
        this.loaiPhong = (loaiPhong != null && !loaiPhong.isBlank()) ? loaiPhong : "LY_THUYET";
        this.thietBi = thietBi;
        this.trangThai = (trangThai != null) ? trangThai : ClassroomStatus.HOAT_DONG;
        this.ghiChu = ghiChu;
        this.ngayTao = LocalDateTime.now();
        this.ngayCapNhat = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public String getTen() {
        return ten;
    }

    public void setTen(String ten) {
        this.ten = ten;
    }

    public String getToaNha() {
        return toaNha;
    }

    public void setToaNha(String toaNha) {
        this.toaNha = toaNha;
    }

    public Integer getTang() {
        return tang;
    }

    public void setTang(Integer tang) {
        this.tang = tang;
    }

    public String getViTri() {
        return viTri;
    }

    public void setViTri(String viTri) {
        this.viTri = viTri;
    }

    public int getSucChua() {
        return sucChua;
    }

    public void setSucChua(int sucChua) {
        this.sucChua = sucChua;
    }

    public String getLoaiPhong() {
        return loaiPhong;
    }

    public void setLoaiPhong(String loaiPhong) {
        this.loaiPhong = loaiPhong;
    }

    public String getThietBi() {
        return thietBi;
    }

    public void setThietBi(String thietBi) {
        this.thietBi = thietBi;
    }

    public ClassroomStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(ClassroomStatus trangThai) {
        this.trangThai = trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }

    public String getNguoiCapNhatUsername() {
        return nguoiCapNhatUsername;
    }

    public void setNguoiCapNhatUsername(String nguoiCapNhatUsername) {
        this.nguoiCapNhatUsername = nguoiCapNhatUsername;
    }

    public boolean isKhaDungChoLichMoi() {
        return this.trangThai == ClassroomStatus.HOAT_DONG;
    }
}
