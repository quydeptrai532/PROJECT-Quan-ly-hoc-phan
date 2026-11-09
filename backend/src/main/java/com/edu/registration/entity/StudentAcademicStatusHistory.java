package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "sinh_vien_lich_su_trang_thai")
public class StudentAcademicStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student sinhVien;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", length = 30)
    private StudentAcademicStatus trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 30)
    private StudentAcademicStatus trangThaiSau;

    @Column(name = "ngay_hieu_luc", nullable = false)
    private LocalDate ngayHieuLuc;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_id")
    private User nguoiThucHien;

    @Column(name = "thoi_diem", nullable = false)
    private LocalDateTime thoiDiem;

    public StudentAcademicStatusHistory() {
    }

    public StudentAcademicStatusHistory(Student sinhVien,
                                       StudentAcademicStatus trangThaiTruoc,
                                       StudentAcademicStatus trangThaiSau,
                                       LocalDate ngayHieuLuc,
                                       String ghiChu,
                                       User nguoiThucHien,
                                       LocalDateTime thoiDiem) {
        this.sinhVien = sinhVien;
        this.trangThaiTruoc = trangThaiTruoc;
        this.trangThaiSau = trangThaiSau;
        this.ngayHieuLuc = ngayHieuLuc;
        this.ghiChu = ghiChu;
        this.nguoiThucHien = nguoiThucHien;
        this.thoiDiem = thoiDiem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getSinhVien() {
        return sinhVien;
    }

    public void setSinhVien(Student sinhVien) {
        this.sinhVien = sinhVien;
    }

    public StudentAcademicStatus getTrangThaiTruoc() {
        return trangThaiTruoc;
    }

    public void setTrangThaiTruoc(StudentAcademicStatus trangThaiTruoc) {
        this.trangThaiTruoc = trangThaiTruoc;
    }

    public StudentAcademicStatus getTrangThaiSau() {
        return trangThaiSau;
    }

    public void setTrangThaiSau(StudentAcademicStatus trangThaiSau) {
        this.trangThaiSau = trangThaiSau;
    }

    public LocalDate getNgayHieuLuc() {
        return ngayHieuLuc;
    }

    public void setNgayHieuLuc(LocalDate ngayHieuLuc) {
        this.ngayHieuLuc = ngayHieuLuc;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public User getNguoiThucHien() {
        return nguoiThucHien;
    }

    public void setNguoiThucHien(User nguoiThucHien) {
        this.nguoiThucHien = nguoiThucHien;
    }

    public LocalDateTime getThoiDiem() {
        return thoiDiem;
    }

    public void setThoiDiem(LocalDateTime thoiDiem) {
        this.thoiDiem = thoiDiem;
    }
}
