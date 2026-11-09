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

import java.time.LocalDateTime;

/**
 * Lịch sử chuyển sinh viên từ lớp học phần này sang lớp học phần khác (QLSV-79 - TASK-AD-04).
 * Lưu vết đầy đủ: lớp nguồn, lớp đích, sinh viên, người thực hiện, thời điểm, lý do và loại quan hệ môn học.
 */
@Entity
@Table(name = "lich_su_chuyen_lop", indexes = {
        @Index(name = "idx_chuyen_lop_nguon", columnList = "lop_nguon_id"),
        @Index(name = "idx_chuyen_lop_dich", columnList = "lop_dich_id"),
        @Index(name = "idx_chuyen_lop_sinh_vien", columnList = "sinh_vien_id"),
        @Index(name = "idx_chuyen_lop_thoi_gian", columnList = "thoi_gian_chuyen")
})
public class ClassTransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_nguon_id", nullable = false)
    private CourseClass lopNguon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_dich_id", nullable = false)
    private CourseClass lopDich;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student sinhVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_thuc_hien_id", nullable = false)
    private User nguoiThucHien;

    @Column(name = "thoi_gian_chuyen", nullable = false)
    private LocalDateTime thoiGianChuyen = LocalDateTime.now();

    @Column(name = "ly_do", length = 500)
    private String lyDo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_nguon_id", nullable = false)
    private Subject monHocNguon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_dich_id", nullable = false)
    private Subject monHocDich;

    @Column(name = "loai_quan_he_mon", length = 50, nullable = false)
    private String loaiQuanHeMon = "CUNG_MON"; // "CUNG_MON" hoặc "TUONG_DUONG"

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    public ClassTransferHistory() {
    }

    public ClassTransferHistory(CourseClass lopNguon, CourseClass lopDich, Student sinhVien,
                                User nguoiThucHien, LocalDateTime thoiGianChuyen, String lyDo,
                                Subject monHocNguon, Subject monHocDich, String loaiQuanHeMon, String ghiChu) {
        this.lopNguon = lopNguon;
        this.lopDich = lopDich;
        this.sinhVien = sinhVien;
        this.nguoiThucHien = nguoiThucHien;
        this.thoiGianChuyen = thoiGianChuyen;
        this.lyDo = lyDo;
        this.monHocNguon = monHocNguon;
        this.monHocDich = monHocDich;
        this.loaiQuanHeMon = loaiQuanHeMon;
        this.ghiChu = ghiChu;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseClass getLopNguon() {
        return lopNguon;
    }

    public void setLopNguon(CourseClass lopNguon) {
        this.lopNguon = lopNguon;
    }

    public CourseClass getLopDich() {
        return lopDich;
    }

    public void setLopDich(CourseClass lopDich) {
        this.lopDich = lopDich;
    }

    public Student getSinhVien() {
        return sinhVien;
    }

    public void setSinhVien(Student sinhVien) {
        this.sinhVien = sinhVien;
    }

    public User getNguoiThucHien() {
        return nguoiThucHien;
    }

    public void setNguoiThucHien(User nguoiThucHien) {
        this.nguoiThucHien = nguoiThucHien;
    }

    public LocalDateTime getThoiGianChuyen() {
        return thoiGianChuyen;
    }

    public void setThoiGianChuyen(LocalDateTime thoiGianChuyen) {
        this.thoiGianChuyen = thoiGianChuyen;
    }

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String lyDo) {
        this.lyDo = lyDo;
    }

    public Subject getMonHocNguon() {
        return monHocNguon;
    }

    public void setMonHocNguon(Subject monHocNguon) {
        this.monHocNguon = monHocNguon;
    }

    public Subject getMonHocDich() {
        return monHocDich;
    }

    public void setMonHocDich(Subject monHocDich) {
        this.monHocDich = monHocDich;
    }

    public String getLoaiQuanHeMon() {
        return loaiQuanHeMon;
    }

    public void setLoaiQuanHeMon(String loaiQuanHeMon) {
        this.loaiQuanHeMon = loaiQuanHeMon;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }
}
