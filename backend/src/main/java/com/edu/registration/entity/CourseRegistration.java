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
@Table(name = "dang_ky_hoc_phan", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sinh_vien_id", "lop_hoc_phan_id"})
}, indexes = {
        @Index(name = "idx_dang_ky_lop", columnList = "lop_hoc_phan_id")
})
public class CourseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student sinhVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private CourseClass lopHocPhan;

    @Column(nullable = false)
    private LocalDateTime thoiGianDangKy = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus trangThai = RegistrationStatus.DA_DANG_KY;

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

    public CourseClass getLopHocPhan() {
        return lopHocPhan;
    }

    public void setLopHocPhan(CourseClass lopHocPhan) {
        this.lopHocPhan = lopHocPhan;
    }

    public LocalDateTime getThoiGianDangKy() {
        return thoiGianDangKy;
    }

    public void setThoiGianDangKy(LocalDateTime thoiGianDangKy) {
        this.thoiGianDangKy = thoiGianDangKy;
    }

    public RegistrationStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(RegistrationStatus trangThai) {
        this.trangThai = trangThai;
    }
}
