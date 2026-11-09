package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khoa_dao_tao")
public class AcademicCohort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String ma;

    @Column(nullable = false, length = 150)
    private String ten;

    @Column(name = "nien_khoa", nullable = false, length = 30)
    private String nienKhoa;

    @Column(name = "nam_bat_dau", nullable = false)
    private int namBatDau;

    @Column(name = "nam_ket_thuc", nullable = false)
    private int namKetThuc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_bat_dau_id")
    private Semester hocKyBatDau;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_ket_thuc_id")
    private Semester hocKyKetThuc;

    @Column(name = "hieu_luc_tu")
    private LocalDate hieuLucTu;

    @Column(name = "hieu_luc_den")
    private LocalDate hieuLucDen;

    @Column(name = "dang_hieu_luc", nullable = false)
    private boolean dangHieuLuc = true;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "nguoi_tao_id")
    private Long nguoiTaoId;

    @Column(name = "nguoi_tao_username", length = 50)
    private String nguoiTaoUsername;

    @Column(name = "tao_luc")
    private LocalDateTime taoLuc;

    @Column(name = "nguoi_cap_nhat_id")
    private Long nguoiCapNhatId;

    @Column(name = "nguoi_cap_nhat_username", length = 50)
    private String nguoiCapNhatUsername;

    @Column(name = "cap_nhat_luc")
    private LocalDateTime capNhatLuc;

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

    public String getNienKhoa() {
        return nienKhoa;
    }

    public void setNienKhoa(String nienKhoa) {
        this.nienKhoa = nienKhoa;
    }

    public int getNamBatDau() {
        return namBatDau;
    }

    public void setNamBatDau(int namBatDau) {
        this.namBatDau = namBatDau;
    }

    public int getNamKetThuc() {
        return namKetThuc;
    }

    public void setNamKetThuc(int namKetThuc) {
        this.namKetThuc = namKetThuc;
    }

    public Semester getHocKyBatDau() {
        return hocKyBatDau;
    }

    public void setHocKyBatDau(Semester hocKyBatDau) {
        this.hocKyBatDau = hocKyBatDau;
    }

    public Semester getHocKyKetThuc() {
        return hocKyKetThuc;
    }

    public void setHocKyKetThuc(Semester hocKyKetThuc) {
        this.hocKyKetThuc = hocKyKetThuc;
    }

    public LocalDate getHieuLucTu() {
        return hieuLucTu;
    }

    public void setHieuLucTu(LocalDate hieuLucTu) {
        this.hieuLucTu = hieuLucTu;
    }

    public LocalDate getHieuLucDen() {
        return hieuLucDen;
    }

    public void setHieuLucDen(LocalDate hieuLucDen) {
        this.hieuLucDen = hieuLucDen;
    }

    public boolean isDangHieuLuc() {
        return dangHieuLuc;
    }

    public void setDangHieuLuc(boolean dangHieuLuc) {
        this.dangHieuLuc = dangHieuLuc;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }

    public Long getNguoiTaoId() {
        return nguoiTaoId;
    }

    public void setNguoiTaoId(Long nguoiTaoId) {
        this.nguoiTaoId = nguoiTaoId;
    }

    public String getNguoiTaoUsername() {
        return nguoiTaoUsername;
    }

    public void setNguoiTaoUsername(String nguoiTaoUsername) {
        this.nguoiTaoUsername = nguoiTaoUsername;
    }

    public LocalDateTime getTaoLuc() {
        return taoLuc;
    }

    public void setTaoLuc(LocalDateTime taoLuc) {
        this.taoLuc = taoLuc;
    }

    public Long getNguoiCapNhatId() {
        return nguoiCapNhatId;
    }

    public void setNguoiCapNhatId(Long nguoiCapNhatId) {
        this.nguoiCapNhatId = nguoiCapNhatId;
    }

    public String getNguoiCapNhatUsername() {
        return nguoiCapNhatUsername;
    }

    public void setNguoiCapNhatUsername(String nguoiCapNhatUsername) {
        this.nguoiCapNhatUsername = nguoiCapNhatUsername;
    }

    public LocalDateTime getCapNhatLuc() {
        return capNhatLuc;
    }

    public void setCapNhatLuc(LocalDateTime capNhatLuc) {
        this.capNhatLuc = capNhatLuc;
    }
}
