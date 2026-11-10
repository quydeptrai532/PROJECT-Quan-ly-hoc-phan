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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phong_hoc_id")
    private Classroom phongHoc;

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

    @Column(name = "phien_ban_bang_diem")
    private Integer phienBanBangDiem = 0;

    @Column(name = "gui_duyet_bang_diem_luc")
    private LocalDateTime guiDuyetBangDiemLuc;

    @Column(name = "nguoi_gui_duyet_bang_diem_id")
    private Long nguoiGuiDuyetBangDiemId;

    @Column(name = "tra_ve_bang_diem_luc")
    private LocalDateTime traVeBangDiemLuc;

    @Column(name = "nguoi_tra_ve_bang_diem_id")
    private Long nguoiTraVeBangDiemId;

    @Column(name = "ly_do_tra_ve_bang_diem", length = 500)
    private String lyDoTraVeBangDiem;

    @Column(name = "duyet_bang_diem_luc")
    private LocalDateTime duyetBangDiemLuc;

    @Column(name = "nguoi_duyet_bang_diem_id")
    private Long nguoiDuyetBangDiemId;

    @Column(name = "cong_bo_bang_diem_luc")
    private LocalDateTime congBoBangDiemLuc;

    @Column(name = "nguoi_cong_bo_bang_diem_id")
    private Long nguoiCongBoBangDiemId;

    @Column(name = "khoa_bang_diem_luc")
    private LocalDateTime khoaBangDiemLuc;

    @Column(name = "nguoi_khoa_bang_diem_id")
    private Long nguoiKhoaBangDiemId;

    @Column(name = "mo_khoa_bang_diem_luc")
    private LocalDateTime moKhoaBangDiemLuc;

    @Column(name = "nguoi_mo_khoa_bang_diem_id")
    private Long nguoiMoKhoaBangDiemId;

    @Column(name = "ly_do_mo_khoa_bang_diem", length = 500)
    private String lyDoMoKhoaBangDiem;

    @Column(name = "nguong_so_buoi_vang")
    private Integer nguongSoBuoiVang;

    @Column(name = "nguong_ty_le_vang")
    private Double nguongTyLeVang;

    @Column(name = "da_huy", nullable = false)
    private boolean daHuy = false;

    @Column(name = "thoi_gian_huy")
    private LocalDateTime thoiGianHuy;

    @Column(name = "ly_do_huy", length = 500)
    private String lyDoHuy;

    @Column(name = "nguoi_huy_id")
    private Long nguoiHuyId;

    @Column(name = "nguoi_huy_username", length = 100)
    private String nguoiHuyUsername;

    public GradeSheetStatus getTrangThaiBangDiemHieuLuc() {
        return trangThaiBangDiem != null ? trangThaiBangDiem : GradeSheetStatus.NHAP;
    }

    /**
     * Sinh viên chỉ được xem điểm khi bảng điểm đã công bố (DA_CONG_BO hoặc DA_KHOA đã công bố).
     * Giữ tương thích ngược cho dữ liệu cũ/test khi trangThaiBangDiem == null.
     */
    public boolean isBangDiemDaCongBo() {
        return trangThaiBangDiem == null
                || trangThaiBangDiem == GradeSheetStatus.DA_CONG_BO
                || trangThaiBangDiem == GradeSheetStatus.DA_KHOA;
    }

    /**
     * Bảng điểm chỉ cho phép nhập/sửa/import điểm khi đang ở trạng thái NHAP hoặc TRA_VE.
     * Khi ở CHO_DUYET, DA_DUYET, DA_CONG_BO hoặc DA_KHOA đều bị khóa chỉnh sửa.
     */
    public boolean isChoPhepSuaDiem() {
        return getTrangThaiBangDiemHieuLuc().isChoPhepSuaDiem();
    }

    /**
     * Bảng điểm đã khóa hoàn toàn ở bước cuối (DA_CONG_BO hoặc DA_KHOA).
     */
    public boolean isBangDiemDaKhoa() {
        return getTrangThaiBangDiemHieuLuc().isDaKhoa();
    }

    public int getPhienBanBangDiemHieuLuc() {
        return phienBanBangDiem != null ? phienBanBangDiem : 0;
    }

    public void tangPhienBanBangDiem() {
        this.phienBanBangDiem = getPhienBanBangDiemHieuLuc() + 1;
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

    public Classroom getPhongHoc() {
        return phongHoc;
    }

    public void setPhongHoc(Classroom phongHoc) {
        this.phongHoc = phongHoc;
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

    public Integer getPhienBanBangDiem() {
        return phienBanBangDiem;
    }

    public void setPhienBanBangDiem(Integer phienBanBangDiem) {
        this.phienBanBangDiem = phienBanBangDiem;
    }

    public LocalDateTime getGuiDuyetBangDiemLuc() {
        return guiDuyetBangDiemLuc;
    }

    public void setGuiDuyetBangDiemLuc(LocalDateTime guiDuyetBangDiemLuc) {
        this.guiDuyetBangDiemLuc = guiDuyetBangDiemLuc;
    }

    public Long getNguoiGuiDuyetBangDiemId() {
        return nguoiGuiDuyetBangDiemId;
    }

    public void setNguoiGuiDuyetBangDiemId(Long nguoiGuiDuyetBangDiemId) {
        this.nguoiGuiDuyetBangDiemId = nguoiGuiDuyetBangDiemId;
    }

    public LocalDateTime getTraVeBangDiemLuc() {
        return traVeBangDiemLuc;
    }

    public void setTraVeBangDiemLuc(LocalDateTime traVeBangDiemLuc) {
        this.traVeBangDiemLuc = traVeBangDiemLuc;
    }

    public Long getNguoiTraVeBangDiemId() {
        return nguoiTraVeBangDiemId;
    }

    public void setNguoiTraVeBangDiemId(Long nguoiTraVeBangDiemId) {
        this.nguoiTraVeBangDiemId = nguoiTraVeBangDiemId;
    }

    public String getLyDoTraVeBangDiem() {
        return lyDoTraVeBangDiem;
    }

    public void setLyDoTraVeBangDiem(String lyDoTraVeBangDiem) {
        this.lyDoTraVeBangDiem = lyDoTraVeBangDiem;
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

    public LocalDateTime getKhoaBangDiemLuc() {
        return khoaBangDiemLuc;
    }

    public void setKhoaBangDiemLuc(LocalDateTime khoaBangDiemLuc) {
        this.khoaBangDiemLuc = khoaBangDiemLuc;
    }

    public Long getNguoiKhoaBangDiemId() {
        return nguoiKhoaBangDiemId;
    }

    public void setNguoiKhoaBangDiemId(Long nguoiKhoaBangDiemId) {
        this.nguoiKhoaBangDiemId = nguoiKhoaBangDiemId;
    }

    public LocalDateTime getMoKhoaBangDiemLuc() {
        return moKhoaBangDiemLuc;
    }

    public void setMoKhoaBangDiemLuc(LocalDateTime moKhoaBangDiemLuc) {
        this.moKhoaBangDiemLuc = moKhoaBangDiemLuc;
    }

    public Long getNguoiMoKhoaBangDiemId() {
        return nguoiMoKhoaBangDiemId;
    }

    public void setNguoiMoKhoaBangDiemId(Long nguoiMoKhoaBangDiemId) {
        this.nguoiMoKhoaBangDiemId = nguoiMoKhoaBangDiemId;
    }

    public String getLyDoMoKhoaBangDiem() {
        return lyDoMoKhoaBangDiem;
    }

    public void setLyDoMoKhoaBangDiem(String lyDoMoKhoaBangDiem) {
        this.lyDoMoKhoaBangDiem = lyDoMoKhoaBangDiem;
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

    public boolean isDaHuy() {
        return daHuy;
    }

    public void setDaHuy(boolean daHuy) {
        this.daHuy = daHuy;
    }

    public LocalDateTime getThoiGianHuy() {
        return thoiGianHuy;
    }

    public void setThoiGianHuy(LocalDateTime thoiGianHuy) {
        this.thoiGianHuy = thoiGianHuy;
    }

    public String getLyDoHuy() {
        return lyDoHuy;
    }

    public void setLyDoHuy(String lyDoHuy) {
        this.lyDoHuy = lyDoHuy;
    }

    public Long getNguoiHuyId() {
        return nguoiHuyId;
    }

    public void setNguoiHuyId(Long nguoiHuyId) {
        this.nguoiHuyId = nguoiHuyId;
    }

    public String getNguoiHuyUsername() {
        return nguoiHuyUsername;
    }

    public void setNguoiHuyUsername(String nguoiHuyUsername) {
        this.nguoiHuyUsername = nguoiHuyUsername;
    }
}
