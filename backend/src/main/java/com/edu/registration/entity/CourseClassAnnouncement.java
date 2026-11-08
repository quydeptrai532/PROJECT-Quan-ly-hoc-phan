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

import java.time.LocalDateTime;

/**
 * Class announcement created by an assigned lecturer for their class (QLSV-68 / TASK-LC-03).
 */
@Entity
@Table(name = "thong_bao_lop_hoc_phan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_thong_bao_lop_idempotency",
                        columnNames = {"lop_hoc_phan_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_thong_bao_lop_hoc_phan", columnList = "lop_hoc_phan_id,thoi_gian_tao"),
                @Index(name = "idx_thong_bao_lop_giang_vien", columnList = "giang_vien_id,thoi_gian_tao")
        })
public class CourseClassAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private CourseClass lopHocPhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "giang_vien_id", nullable = false)
    private Lecturer giangVien;

    @Column(name = "tieu_de", nullable = false, length = 120)
    private String tieuDe;

    @Column(name = "noi_dung", nullable = false, length = 1000)
    private String noiDung;

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Column(name = "so_nguoi_nhan", nullable = false)
    private int soNguoiNhan;

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

    public String getTieuDe() {
        return tieuDe;
    }

    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public int getSoNguoiNhan() {
        return soNguoiNhan;
    }

    public void setSoNguoiNhan(int soNguoiNhan) {
        this.soNguoiNhan = soNguoiNhan;
    }

    public LocalDateTime getThoiGianTao() {
        return thoiGianTao;
    }

    public void setThoiGianTao(LocalDateTime thoiGianTao) {
        this.thoiGianTao = thoiGianTao;
    }
}
