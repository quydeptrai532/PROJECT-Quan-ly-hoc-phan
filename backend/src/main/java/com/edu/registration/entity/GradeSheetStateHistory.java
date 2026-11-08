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

import java.time.LocalDateTime;

@Entity
@Table(name = "lich_su_trang_thai_bang_diem", indexes = {
        @Index(name = "idx_ls_bang_diem_lop", columnList = "lop_hoc_phan_id"),
        @Index(name = "idx_ls_bang_diem_thoi_diem", columnList = "thoi_diem")
})
public class GradeSheetStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private CourseClass lopHocPhan;

    @Column(name = "hanh_dong", nullable = false, length = 32)
    private String hanhDong;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_truoc", nullable = false, length = 24)
    private GradeSheetStatus trangThaiTruoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_sau", nullable = false, length = 24)
    private GradeSheetStatus trangThaiSau;

    @Column(name = "ly_do", length = 500)
    private String lyDo;

    @Column(name = "nguoi_thuc_hien_id")
    private Long nguoiThucHienId;

    @Column(name = "nguoi_thuc_hien_username", length = 50)
    private String nguoiThucHienUsername;

    @Column(name = "nguoi_thuc_hien_role", length = 24)
    private String nguoiThucHienRole;

    @Column(name = "thoi_diem", nullable = false)
    private LocalDateTime thoiDiem;

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

    public String getHanhDong() {
        return hanhDong;
    }

    public void setHanhDong(String hanhDong) {
        this.hanhDong = hanhDong;
    }

    public GradeSheetStatus getTrangThaiTruoc() {
        return trangThaiTruoc;
    }

    public void setTrangThaiTruoc(GradeSheetStatus trangThaiTruoc) {
        this.trangThaiTruoc = trangThaiTruoc;
    }

    public GradeSheetStatus getTrangThaiSau() {
        return trangThaiSau;
    }

    public void setTrangThaiSau(GradeSheetStatus trangThaiSau) {
        this.trangThaiSau = trangThaiSau;
    }

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String lyDo) {
        this.lyDo = lyDo;
    }

    public Long getNguoiThucHienId() {
        return nguoiThucHienId;
    }

    public void setNguoiThucHienId(Long nguoiThucHienId) {
        this.nguoiThucHienId = nguoiThucHienId;
    }

    public String getNguoiThucHienUsername() {
        return nguoiThucHienUsername;
    }

    public void setNguoiThucHienUsername(String nguoiThucHienUsername) {
        this.nguoiThucHienUsername = nguoiThucHienUsername;
    }

    public String getNguoiThucHienRole() {
        return nguoiThucHienRole;
    }

    public void setNguoiThucHienRole(String nguoiThucHienRole) {
        this.nguoiThucHienRole = nguoiThucHienRole;
    }

    public LocalDateTime getThoiDiem() {
        return thoiDiem;
    }

    public void setThoiDiem(LocalDateTime thoiDiem) {
        this.thoiDiem = thoiDiem;
    }
}
