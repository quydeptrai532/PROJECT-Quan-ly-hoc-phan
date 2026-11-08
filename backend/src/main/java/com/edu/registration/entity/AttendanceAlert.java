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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "canh_bao_diem_danh",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_canh_bao_sv_lop_nguong",
                columnNames = {"sinh_vien_id", "lop_hoc_phan_id", "loai_nguong"}
        ),
        indexes = {
                @Index(name = "idx_canh_bao_lop", columnList = "lop_hoc_phan_id"),
                @Index(name = "idx_canh_bao_sinh_vien", columnList = "sinh_vien_id"),
                @Index(name = "idx_canh_bao_trang_thai", columnList = "trang_thai")
        })
public class AttendanceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private CourseClass lopHocPhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student sinhVien;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_nguong", nullable = false, length = 30)
    private AlertThresholdType loaiNguong;

    @Column(name = "gia_tri_nguong", nullable = false)
    private Double giaTriNguong;

    @Column(name = "so_buoi_vang", nullable = false)
    private int soBuoiVang;

    @Column(name = "tong_buoi_hop_le", nullable = false)
    private int tongBuoiHopLe;

    @Column(name = "ty_le_vang", nullable = false)
    private double tyLeVang;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private AlertStatus trangThai;

    @Column(name = "thong_diep", nullable = false, length = 500)
    private String thongDiep;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao = LocalDateTime.now();

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

    public Student getSinhVien() {
        return sinhVien;
    }

    public void setSinhVien(Student sinhVien) {
        this.sinhVien = sinhVien;
    }

    public AlertThresholdType getLoaiNguong() {
        return loaiNguong;
    }

    public void setLoaiNguong(AlertThresholdType loaiNguong) {
        this.loaiNguong = loaiNguong;
    }

    public Double getGiaTriNguong() {
        return giaTriNguong;
    }

    public void setGiaTriNguong(Double giaTriNguong) {
        this.giaTriNguong = giaTriNguong;
    }

    public int getSoBuoiVang() {
        return soBuoiVang;
    }

    public void setSoBuoiVang(int soBuoiVang) {
        this.soBuoiVang = soBuoiVang;
    }

    public int getTongBuoiHopLe() {
        return tongBuoiHopLe;
    }

    public void setTongBuoiHopLe(int tongBuoiHopLe) {
        this.tongBuoiHopLe = tongBuoiHopLe;
    }

    public double getTyLeVang() {
        return tyLeVang;
    }

    public void setTyLeVang(double tyLeVang) {
        this.tyLeVang = tyLeVang;
    }

    public AlertStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(AlertStatus trangThai) {
        this.trangThai = trangThai;
    }

    public String getThongDiep() {
        return thongDiep;
    }

    public void setThongDiep(String thongDiep) {
        this.thongDiep = thongDiep;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getThoiGianTao() {
        return thoiGianTao;
    }

    public void setThoiGianTao(LocalDateTime thoiGianTao) {
        this.thoiGianTao = thoiGianTao;
    }

    public LocalDateTime getThoiGianCapNhat() {
        return thoiGianCapNhat;
    }

    public void setThoiGianCapNhat(LocalDateTime thoiGianCapNhat) {
        this.thoiGianCapNhat = thoiGianCapNhat;
    }
}
