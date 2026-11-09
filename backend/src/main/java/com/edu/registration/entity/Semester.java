package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "hoc_ky")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ten;

    @Column(nullable = false, length = 20)
    private String namHoc;

    @Column(nullable = false)
    private LocalDate ngayBatDau;

    @Column(nullable = false)
    private LocalDate ngayKetThuc;

    @Column(nullable = false)
    private LocalDate dangKyTu;

    @Column(nullable = false)
    private LocalDate dangKyDen;

    @Column(nullable = false)
    private int soTinChiToiThieu = 0;

    @Column(nullable = false)
    private int soTinChiToiDa = 24;

    public boolean dangMoDangKy() {
        LocalDate homNay = LocalDate.now();
        return !homNay.isBefore(dangKyTu) && !homNay.isAfter(dangKyDen);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTen() {
        return ten;
    }

    public void setTen(String ten) {
        this.ten = ten;
    }

    public String getNamHoc() {
        return namHoc;
    }

    public void setNamHoc(String namHoc) {
        this.namHoc = namHoc;
    }

    public LocalDate getNgayBatDau() {
        return ngayBatDau;
    }

    public void setNgayBatDau(LocalDate ngayBatDau) {
        this.ngayBatDau = ngayBatDau;
    }

    public LocalDate getNgayKetThuc() {
        return ngayKetThuc;
    }

    public void setNgayKetThuc(LocalDate ngayKetThuc) {
        this.ngayKetThuc = ngayKetThuc;
    }

    public LocalDate getDangKyTu() {
        return dangKyTu;
    }

    public void setDangKyTu(LocalDate dangKyTu) {
        this.dangKyTu = dangKyTu;
    }

    public LocalDate getDangKyDen() {
        return dangKyDen;
    }

    public void setDangKyDen(LocalDate dangKyDen) {
        this.dangKyDen = dangKyDen;
    }

    public int getSoTinChiToiThieu() {
        return soTinChiToiThieu;
    }

    public void setSoTinChiToiThieu(int soTinChiToiThieu) {
        this.soTinChiToiThieu = soTinChiToiThieu;
    }

    public int getSoTinChiToiDa() {
        return soTinChiToiDa;
    }

    public void setSoTinChiToiDa(int soTinChiToiDa) {
        this.soTinChiToiDa = soTinChiToiDa;
    }
}
