package com.edu.registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_su_dang_ky", indexes = {
        @Index(name = "idx_lich_su_sv_hk_time", columnList = "sinh_vien_id,hoc_ky_id,occurred_at,id")
})
public class RegistrationHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student sinhVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private CourseClass lopHocPhan;

    @Column(name = "hoc_ky_id", nullable = false)
    private Long hocKyId;

    @Column(name = "dang_ky_id")
    private Long dangKyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus trangThai;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime thoiGianDangKy;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime thoiDiem;

    @Column(name = "cancelled_at")
    private LocalDateTime thoiGianHuy;

    @Column(name = "cancellation_reason", length = 255)
    private String lyDoHuy;

    public Long getId() { return id; }
    public Student getSinhVien() { return sinhVien; }
    public Long getDangKyId() { return dangKyId; }
    public CourseClass getLopHocPhan() { return lopHocPhan; }
    public RegistrationStatus getTrangThai() { return trangThai; }
    public LocalDateTime getThoiGianDangKy() { return thoiGianDangKy; }
    public LocalDateTime getThoiDiem() { return thoiDiem; }
    public LocalDateTime getThoiGianHuy() { return thoiGianHuy; }
    public String getLyDoHuy() { return lyDoHuy; }
    public void setSinhVien(Student value) { sinhVien = value; }
    public void setLopHocPhan(CourseClass value) { lopHocPhan = value; }
    public void setHocKyId(Long value) { hocKyId = value; }
    public void setDangKyId(Long value) { dangKyId = value; }
    public void setTrangThai(RegistrationStatus value) { trangThai = value; }
    public void setThoiGianDangKy(LocalDateTime value) { thoiGianDangKy = value; }
    public void setThoiDiem(LocalDateTime value) { thoiDiem = value; }
    public void setThoiGianHuy(LocalDateTime value) { thoiGianHuy = value; }
    public void setLyDoHuy(String value) { lyDoHuy = value; }
}
