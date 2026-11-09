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
@Table(name = "thong_bao_nghi",
        uniqueConstraints = @UniqueConstraint(name = "uk_thong_bao_nghi_lop_ngay",
                columnNames = {"lop_hoc_phan_id", "ngay_nghi"}),
        indexes = {
                @Index(name = "idx_thong_bao_nghi_lop", columnList = "lop_hoc_phan_id"),
                @Index(name = "idx_thong_bao_nghi_ngay", columnList = "ngay_nghi")
        })
public class ClassAbsenceNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private CourseClass lopHocPhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giang_vien_id")
    private Lecturer giangVien;

    @Column(name = "ngay_nghi", nullable = false)
    private LocalDate ngayNghi;

    @Column(length = 255)
    private String lyDo;

    @Column(name = "thoi_gian_tao", nullable = false)
    private LocalDateTime thoiGianTao = LocalDateTime.now();

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

    public Lecturer getGiangVien() {
        return giangVien;
    }

    public void setGiangVien(Lecturer giangVien) {
        this.giangVien = giangVien;
    }

    public LocalDate getNgayNghi() {
        return ngayNghi;
    }

    public void setNgayNghi(LocalDate ngayNghi) {
        this.ngayNghi = ngayNghi;
    }

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String lyDo) {
        this.lyDo = lyDo;
    }

    public LocalDateTime getThoiGianTao() {
        return thoiGianTao;
    }

    public void setThoiGianTao(LocalDateTime thoiGianTao) {
        this.thoiGianTao = thoiGianTao;
    }
}
