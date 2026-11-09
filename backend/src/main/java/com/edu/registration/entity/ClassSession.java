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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "buoi_hoc",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_buoi_hoc_lop_ngay_ca",
                columnNames = {"lop_hoc_phan_id", "ngay_hoc", "ca_hoc"}
        ),
        indexes = {
                @Index(name = "idx_buoi_hoc_lop", columnList = "lop_hoc_phan_id"),
                @Index(name = "idx_buoi_hoc_ngay", columnList = "ngay_hoc")
        })
public class ClassSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private CourseClass lopHocPhan;

    @Column(name = "ngay_hoc", nullable = false)
    private LocalDate ngayHoc;

    @Column(name = "ca_hoc", nullable = false)
    private Integer caHoc;

    @Column(name = "phong_hoc", length = 30)
    private String phongHoc;

    @Column(name = "tiet_bat_dau")
    private Integer tietBatDau;

    @Column(name = "so_tiet")
    private Integer soTiet;

    @Column(name = "noi_dung", length = 255)
    private String noiDung;

    @Column(name = "da_khoa", nullable = false)
    private boolean daKhoa = false;

    @Column(name = "thoi_gian_khoa")
    private LocalDateTime thoiGianKhoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_khoa_user_id")
    private User nguoiKhoa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_tao_user_id", nullable = false)
    private User nguoiTao;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_cap_nhat_user_id")
    private User nguoiCapNhat;

    @Column(name = "thoi_gian_cap_nhat")
    private LocalDateTime thoiGianCapNhat;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseClass getLopHocPhan() {
        return lopHocPhan;
    }

    public void setLopHocPhan(CourseClass lopHocPhan) {
        this.lopHocPhan = lopHocPhan;
    }

    public LocalDate getNgayHoc() {
        return ngayHoc;
    }

    public void setNgayHoc(LocalDate ngayHoc) {
        this.ngayHoc = ngayHoc;
    }

    public Integer getCaHoc() {
        return caHoc;
    }

    public void setCaHoc(Integer caHoc) {
        this.caHoc = caHoc;
    }

    public String getPhongHoc() {
        return phongHoc;
    }

    public void setPhongHoc(String phongHoc) {
        this.phongHoc = phongHoc;
    }

    public Integer getTietBatDau() {
        return tietBatDau;
    }

    public void setTietBatDau(Integer tietBatDau) {
        this.tietBatDau = tietBatDau;
    }

    public Integer getSoTiet() {
        return soTiet;
    }

    public void setSoTiet(Integer soTiet) {
        this.soTiet = soTiet;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public boolean isDaKhoa() {
        return daKhoa;
    }

    public void setDaKhoa(boolean daKhoa) {
        this.daKhoa = daKhoa;
    }

    public LocalDateTime getThoiGianKhoa() {
        return thoiGianKhoa;
    }

    public void setThoiGianKhoa(LocalDateTime thoiGianKhoa) {
        this.thoiGianKhoa = thoiGianKhoa;
    }

    public User getNguoiKhoa() {
        return nguoiKhoa;
    }

    public void setNguoiKhoa(User nguoiKhoa) {
        this.nguoiKhoa = nguoiKhoa;
    }

    public User getNguoiTao() {
        return nguoiTao;
    }

    public void setNguoiTao(User nguoiTao) {
        this.nguoiTao = nguoiTao;
    }

    public LocalDateTime getThoiGianTao() {
        return thoiGianTao;
    }

    public void setThoiGianTao(LocalDateTime thoiGianTao) {
        this.thoiGianTao = thoiGianTao;
    }

    public User getNguoiCapNhat() {
        return nguoiCapNhat;
    }

    public void setNguoiCapNhat(User nguoiCapNhat) {
        this.nguoiCapNhat = nguoiCapNhat;
    }

    public LocalDateTime getThoiGianCapNhat() {
        return thoiGianCapNhat;
    }

    public void setThoiGianCapNhat(LocalDateTime thoiGianCapNhat) {
        this.thoiGianCapNhat = thoiGianCapNhat;
    }
}
