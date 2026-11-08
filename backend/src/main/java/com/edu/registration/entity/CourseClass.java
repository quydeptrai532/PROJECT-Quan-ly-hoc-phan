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
@Table(name = "lop_hoc_phan", indexes = {
        @Index(name = "idx_lop_hoc_ky", columnList = "hoc_ky_id"),
        @Index(name = "idx_lop_giang_vien", columnList = "giang_vien_id"),
        @Index(name = "idx_lop_mon_hoc", columnList = "mon_hoc_id")
})
public class CourseClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String ma;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_id", nullable = false)
    private Subject monHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoc_ky_id", nullable = false)
    private Semester hocKy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "giang_vien_id", nullable = false)
    private Lecturer giangVien;

    @Column(nullable = false, length = 20)
    private String phong;

    @Column(nullable = false)
    private int thu;

    @Column(nullable = false)
    private int tietBatDau;

    @Column(nullable = false)
    private int soTiet;

    @Column(nullable = false)
    private int siSoToiDa;

    @Column(nullable = false)
    private int siSoHienTai = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_bang_diem", length = 24)
    private GradeSheetStatus trangThaiBangDiem;

    @Column(name = "phien_ban_cong_bo", nullable = false)
    private int phienBanCongBo = 1;

    @Column(name = "gui_duyet_bang_diem_luc")
    private LocalDateTime guiDuyetBangDiemLuc;

    @Column(name = "duyet_bang_diem_luc")
    private LocalDateTime duyetBangDiemLuc;

    @Column(name = "nguoi_duyet_bang_diem_id")
    private Long nguoiDuyetBangDiemId;

    @Column(name = "cong_bo_bang_diem_luc")
    private LocalDateTime congBoBangDiemLuc;

    @Column(name = "nguoi_cong_bo_bang_diem_id")
    private Long nguoiCongBoBangDiemId;

    @Column(name = "nguong_so_buoi_vang")
    private Integer nguongSoBuoiVang;

    @Column(name = "nguong_ty_le_vang")
    private Double nguongTyLeVang;

    public GradeSheetStatus getTrangThaiBangDiemHieuLuc() {
        return trangThaiBangDiem != null ? trangThaiBangDiem : GradeSheetStatus.NHAP;
    }

    public boolean isBangDiemDaCongBo() {
        return trangThaiBangDiem == null || trangThaiBangDiem == GradeSheetStatus.DA_CONG_BO;
    }

    public int getTietKetThuc() {
        return tietBatDau + soTiet - 1;
    }

    public int getConLai() {
        return Math.max(0, siSoToiDa - siSoHienTai);
    }

    public boolean trungLichVoi(CourseClass khac) {
        if (khac == null || thu != khac.getThu()) {
            return false;
        }
        return tietBatDau <= khac.getTietKetThuc() && khac.getTietBatDau() <= getTietKetThuc();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMa() {
        return ma;
    }

    public void setMa(String ma) {
        this.ma = ma;
    }

    public Subject getMonHoc() {
        return monHoc;
    }

    public void setMonHoc(Subject monHoc) {
        this.monHoc = monHoc;
    }

    public Semester getHocKy() {
        return hocKy;
    }

    public void setHocKy(Semester hocKy) {
        this.hocKy = hocKy;
    }

    public Lecturer getGiangVien() {
        return giangVien;
    }

    public void setGiangVien(Lecturer giangVien) {
        this.giangVien = giangVien;
    }

    public String getPhong() {
        return phong;
    }

    public void setPhong(String phong) {
        this.phong = phong;
    }

    public int getThu() {
        return thu;
    }

    public void setThu(int thu) {
        this.thu = thu;
    }

    public int getTietBatDau() {
        return tietBatDau;
    }

    public void setTietBatDau(int tietBatDau) {
        this.tietBatDau = tietBatDau;
    }

    public int getSoTiet() {
        return soTiet;
    }

    public void setSoTiet(int soTiet) {
        this.soTiet = soTiet;
    }

    public int getSiSoToiDa() {
        return siSoToiDa;
    }

    public void setSiSoToiDa(int siSoToiDa) {
        this.siSoToiDa = siSoToiDa;
    }

    public int getSiSoHienTai() {
        return siSoHienTai;
    }

    public void setSiSoHienTai(int siSoHienTai) {
        this.siSoHienTai = siSoHienTai;
    }

    public GradeSheetStatus getTrangThaiBangDiem() {
        return trangThaiBangDiem;
    }

    public void setTrangThaiBangDiem(GradeSheetStatus trangThaiBangDiem) {
        this.trangThaiBangDiem = trangThaiBangDiem;
    }

    public int getPhienBanCongBo() {
        return phienBanCongBo;
    }

    public void setPhienBanCongBo(int phienBanCongBo) {
        this.phienBanCongBo = phienBanCongBo;
    }

    public LocalDateTime getGuiDuyetBangDiemLuc() {
        return guiDuyetBangDiemLuc;
    }

    public void setGuiDuyetBangDiemLuc(LocalDateTime guiDuyetBangDiemLuc) {
        this.guiDuyetBangDiemLuc = guiDuyetBangDiemLuc;
    }

    public LocalDateTime getDuyetBangDiemLuc() {
        return duyetBangDiemLuc;
    }

    public void setDuyetBangDiemLuc(LocalDateTime duyetBangDiemLuc) {
        this.duyetBangDiemLuc = duyetBangDiemLuc;
    }

    public Long getNguoiDuyetBangDiemId() {
        return nguoiDuyetBangDiemId;
    }

    public void setNguoiDuyetBangDiemId(Long nguoiDuyetBangDiemId) {
        this.nguoiDuyetBangDiemId = nguoiDuyetBangDiemId;
    }

    public LocalDateTime getCongBoBangDiemLuc() {
        return congBoBangDiemLuc;
    }

    public void setCongBoBangDiemLuc(LocalDateTime congBoBangDiemLuc) {
        this.congBoBangDiemLuc = congBoBangDiemLuc;
    }

    public Long getNguoiCongBoBangDiemId() {
        return nguoiCongBoBangDiemId;
    }

    public void setNguoiCongBoBangDiemId(Long nguoiCongBoBangDiemId) {
        this.nguoiCongBoBangDiemId = nguoiCongBoBangDiemId;
    }

    public Integer getNguongSoBuoiVang() {
        return nguongSoBuoiVang;
    }

    public void setNguongSoBuoiVang(Integer nguongSoBuoiVang) {
        this.nguongSoBuoiVang = nguongSoBuoiVang;
    }

    public Double getNguongTyLeVang() {
        return nguongTyLeVang;
    }

    public void setNguongTyLeVang(Double nguongTyLeVang) {
        this.nguongTyLeVang = nguongTyLeVang;
    }
}
