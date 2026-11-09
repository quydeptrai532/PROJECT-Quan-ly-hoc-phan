package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "diem")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dang_ky_id", nullable = false, unique = true)
    private CourseRegistration dangKy;

    private Double diemQuaTrinh;

    private Double diemCuoiKy;

    private Double diemTongKet;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(nullable = false)
    private LocalDateTime capNhatLuc = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseRegistration getDangKy() {
        return dangKy;
    }

    public void setDangKy(CourseRegistration dangKy) {
        this.dangKy = dangKy;
    }

    public Double getDiemQuaTrinh() {
        return diemQuaTrinh;
    }

    public void setDiemQuaTrinh(Double diemQuaTrinh) {
        this.diemQuaTrinh = diemQuaTrinh;
    }

    public Double getDiemCuoiKy() {
        return diemCuoiKy;
    }

    public void setDiemCuoiKy(Double diemCuoiKy) {
        this.diemCuoiKy = diemCuoiKy;
    }

    public Double getDiemTongKet() {
        return diemTongKet;
    }

    public void setDiemTongKet(Double diemTongKet) {
        this.diemTongKet = diemTongKet;
    }

    public Long getVersion() {
        return version != null ? version : 0L;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCapNhatLuc() {
        return capNhatLuc;
    }

    public void setCapNhatLuc(LocalDateTime capNhatLuc) {
        this.capNhatLuc = capNhatLuc;
    }
}
