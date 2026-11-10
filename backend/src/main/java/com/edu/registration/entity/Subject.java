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

import java.time.LocalDateTime;

@Entity
@Table(name = "mon_hoc")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String ma;

    @Column(nullable = false, length = 150)
    private String ten;

    @Column(nullable = false)
    private int soTinChi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khoa_id", nullable = false)
    private Faculty khoa;

    @Column(name = "dang_hieu_luc", nullable = false)
    private boolean dangHieuLuc = true;

    @Column(name = "ngay_luu_tru")
    private LocalDateTime ngayLuuTru;

    @Column(name = "ly_do_luu_tru", length = 500)
    private String lyDoLuuTru;

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

    public int getSoTinChi() {
        return soTinChi;
    }

    public void setSoTinChi(int soTinChi) {
        this.soTinChi = soTinChi;
    }

    public Faculty getKhoa() {
        return khoa;
    }

    public void setKhoa(Faculty khoa) {
        this.khoa = khoa;
    }

    public boolean isDangHieuLuc() {
        return dangHieuLuc;
    }

    public void setDangHieuLuc(boolean dangHieuLuc) {
        this.dangHieuLuc = dangHieuLuc;
    }

    public LocalDateTime getNgayLuuTru() {
        return ngayLuuTru;
    }

    public void setNgayLuuTru(LocalDateTime ngayLuuTru) {
        this.ngayLuuTru = ngayLuuTru;
    }

    public String getLyDoLuuTru() {
        return lyDoLuuTru;
    }

    public void setLyDoLuuTru(String lyDoLuuTru) {
        this.lyDoLuuTru = lyDoLuuTru;
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
