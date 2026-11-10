package com.edu.registration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "dot_dang_ky")
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoc_ky_id", nullable = false)
    private Semester hocKy;

    @Column(name = "ma_dot", nullable = false, length = 40)
    private String maDot;

    @Column(name = "ten_dot", nullable = false, length = 150)
    private String tenDot;

    @Column(name = "loai_dot", nullable = false, length = 30)
    private String loaiDot = "CHINH_THUC";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoa_hoc_id")
    private AcademicCohort khoaHoc;

    @Column(name = "khoa_nhap_hoc", length = 50)
    private String khoaNhapHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nganh_id")
    private Major nganh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_hanh_chinh_id")
    private AdministrativeClass lopHanhChinh;

    @Column(name = "nhom_doi_tuong", length = 100)
    private String nhomDoiTuong;

    @Column(name = "thu_tu_uu_tien", nullable = false)
    private int thuTuUuTien = 1;

    @Column(name = "thoi_gian_bat_dau", nullable = false)
    private LocalDateTime thoiGianBatDau;

    @Column(name = "thoi_gian_ket_thuc", nullable = false)
    private LocalDateTime thoiGianKetThuc;

    @Column(name = "dang_hieu_luc", nullable = false)
    private boolean dangHieuLuc = true;

    @Column(name = "dong_som", nullable = false)
    private boolean dongSom = false;

    @Column(name = "thoi_diem_dong_som")
    private LocalDateTime thoiDiemDongSom;

    @Column(name = "ly_do_dong_som", length = 500)
    private String lyDoDongSom;

    @Column(name = "thoi_gian_ket_thuc_goc")
    private LocalDateTime thoiGianKetThucGoc;

    @Column(name = "ly_do_dieu_chinh", length = 500)
    private String lyDoDieuChinh;

    @Column(name = "ghi_chu", length = 500)
    private String ghiChu;

    @Column(name = "nguoi_tao_id")
    private Long nguoiTaoId;

    @Column(name = "nguoi_tao_username", length = 50)
    private String nguoiTaoUsername;

    @Column(name = "tao_luc")
    private LocalDateTime taoLuc;

    @Column(name = "nguoi_cap_nhat_id")
    private Long nguoiCapNhatId;

    @Column(name = "nguoi_cap_nhat_username", length = 50)
    private String nguoiCapNhatUsername;

    @Column(name = "cap_nhat_luc")
    private LocalDateTime capNhatLuc;

    public boolean dangMo(LocalDateTime at) {
        if (!dangHieuLuc || dongSom) {
            return false;
        }
        if (at == null) {
            return false;
        }
        return !at.isBefore(thoiGianBatDau) && !at.isAfter(thoiGianKetThuc);
    }

    public boolean phuHopVoi(Student sv, String cohortCuaSv) {
        if (sv == null) {
            return false;
        }

        // 1. Kiểm tra khóa học / niên khóa
        if (khoaHoc != null) {
            if (cohortCuaSv == null || cohortCuaSv.isBlank()) {
                return false;
            }
            boolean khopMa = khoaHoc.getMa() != null && khoaHoc.getMa().equalsIgnoreCase(cohortCuaSv.trim());
            boolean khopNienKhoa = khoaHoc.getNienKhoa() != null && khoaHoc.getNienKhoa().equalsIgnoreCase(cohortCuaSv.trim());
            if (!khopMa && !khopNienKhoa) {
                return false;
            }
        }

        if (khoaNhapHoc != null && !khoaNhapHoc.isBlank()) {
            if (cohortCuaSv == null || !khoaNhapHoc.trim().equalsIgnoreCase(cohortCuaSv.trim())) {
                return false;
            }
        }

        // 2. Kiểm tra ngành
        if (nganh != null) {
            if (sv.getNganh() == null || !nganh.getId().equals(sv.getNganh().getId())) {
                return false;
            }
        }

        // 3. Kiểm tra lớp hành chính
        if (lopHanhChinh != null) {
            if (sv.getLopHanhChinh() == null || !lopHanhChinh.getId().equals(sv.getLopHanhChinh().getId())) {
                return false;
            }
        }

        // 4. Nhóm đối tượng (nếu có nhãn đặc thù như TOAN_TRUONG thì cho phép mọi sinh viên)
        if (nhomDoiTuong != null && !nhomDoiTuong.isBlank()) {
            String nhom = nhomDoiTuong.trim();
            if (!nhom.equalsIgnoreCase("TOAN_TRUONG") && !nhom.equalsIgnoreCase("ALL")) {
                // Nhóm đối tượng đặc thù được đối chiếu theo lớp/ngành hoặc thuộc tính nhóm
                boolean khopLop = sv.getLopHanhChinh() != null && sv.getLopHanhChinh().getMa().equalsIgnoreCase(nhom);
                boolean khopNganh = sv.getNganh() != null && sv.getNganh().getMa().equalsIgnoreCase(nhom);
                boolean khopKhoa = cohortCuaSv != null && cohortCuaSv.equalsIgnoreCase(nhom);
                if (!khopLop && !khopNganh && !khopKhoa) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean laToanTruong() {
        return khoaHoc == null && (khoaNhapHoc == null || khoaNhapHoc.isBlank())
                && nganh == null && lopHanhChinh == null
                && (nhomDoiTuong == null || nhomDoiTuong.isBlank() || nhomDoiTuong.equalsIgnoreCase("TOAN_TRUONG") || nhomDoiTuong.equalsIgnoreCase("ALL"));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Semester getHocKy() {
        return hocKy;
    }

    public void setHocKy(Semester hocKy) {
        this.hocKy = hocKy;
    }

    public String getMaDot() {
        return maDot;
    }

    public void setMaDot(String maDot) {
        this.maDot = maDot;
    }

    public String getTenDot() {
        return tenDot;
    }

    public void setTenDot(String tenDot) {
        this.tenDot = tenDot;
    }

    public String getLoaiDot() {
        return loaiDot;
    }

    public void setLoaiDot(String loaiDot) {
        this.loaiDot = loaiDot;
    }

    public AcademicCohort getKhoaHoc() {
        return khoaHoc;
    }

    public void setKhoaHoc(AcademicCohort khoaHoc) {
        this.khoaHoc = khoaHoc;
    }

    public String getKhoaNhapHoc() {
        return khoaNhapHoc;
    }

    public void setKhoaNhapHoc(String khoaNhapHoc) {
        this.khoaNhapHoc = khoaNhapHoc;
    }

    public Major getNganh() {
        return nganh;
    }

    public void setNganh(Major nganh) {
        this.nganh = nganh;
    }

    public AdministrativeClass getLopHanhChinh() {
        return lopHanhChinh;
    }

    public void setLopHanhChinh(AdministrativeClass lopHanhChinh) {
        this.lopHanhChinh = lopHanhChinh;
    }

    public String getNhomDoiTuong() {
        return nhomDoiTuong;
    }

    public void setNhomDoiTuong(String nhomDoiTuong) {
        this.nhomDoiTuong = nhomDoiTuong;
    }

    public int getThuTuUuTien() {
        return thuTuUuTien;
    }

    public void setThuTuUuTien(int thuTuUuTien) {
        this.thuTuUuTien = thuTuUuTien;
    }

    public LocalDateTime getThoiGianBatDau() {
        return thoiGianBatDau;
    }

    public void setThoiGianBatDau(LocalDateTime thoiGianBatDau) {
        this.thoiGianBatDau = thoiGianBatDau;
    }

    public LocalDateTime getThoiGianKetThuc() {
        return thoiGianKetThuc;
    }

    public void setThoiGianKetThuc(LocalDateTime thoiGianKetThuc) {
        this.thoiGianKetThuc = thoiGianKetThuc;
    }

    public boolean isDangHieuLuc() {
        return dangHieuLuc;
    }

    public void setDangHieuLuc(boolean dangHieuLuc) {
        this.dangHieuLuc = dangHieuLuc;
    }

    public boolean isDongSom() {
        return dongSom;
    }

    public void setDongSom(boolean dongSom) {
        this.dongSom = dongSom;
    }

    public LocalDateTime getThoiDiemDongSom() {
        return thoiDiemDongSom;
    }

    public void setThoiDiemDongSom(LocalDateTime thoiDiemDongSom) {
        this.thoiDiemDongSom = thoiDiemDongSom;
    }

    public String getLyDoDongSom() {
        return lyDoDongSom;
    }

    public void setLyDoDongSom(String lyDoDongSom) {
        this.lyDoDongSom = lyDoDongSom;
    }

    public LocalDateTime getThoiGianKetThucGoc() {
        return thoiGianKetThucGoc;
    }

    public void setThoiGianKetThucGoc(LocalDateTime thoiGianKetThucGoc) {
        this.thoiGianKetThucGoc = thoiGianKetThucGoc;
    }

    public String getLyDoDieuChinh() {
        return lyDoDieuChinh;
    }

    public void setLyDoDieuChinh(String lyDoDieuChinh) {
        this.lyDoDieuChinh = lyDoDieuChinh;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public Long getNguoiTaoId() {
        return nguoiTaoId;
    }

    public void setNguoiTaoId(Long nguoiTaoId) {
        this.nguoiTaoId = nguoiTaoId;
    }

    public String getNguoiTaoUsername() {
        return nguoiTaoUsername;
    }

    public void setNguoiTaoUsername(String nguoiTaoUsername) {
        this.nguoiTaoUsername = nguoiTaoUsername;
    }

    public LocalDateTime getTaoLuc() {
        return taoLuc;
    }

    public void setTaoLuc(LocalDateTime taoLuc) {
        this.taoLuc = taoLuc;
    }

    public Long getNguoiCapNhatId() {
        return nguoiCapNhatId;
    }

    public void setNguoiCapNhatId(Long nguoiCapNhatId) {
        this.nguoiCapNhatId = nguoiCapNhatId;
    }

    public String getNguoiCapNhatUsername() {
        return nguoiCapNhatUsername;
    }

    public void setNguoiCapNhatUsername(String nguoiCapNhatUsername) {
        this.nguoiCapNhatUsername = nguoiCapNhatUsername;
    }

    public LocalDateTime getCapNhatLuc() {
        return capNhatLuc;
    }

    public void setCapNhatLuc(LocalDateTime capNhatLuc) {
        this.capNhatLuc = capNhatLuc;
    }
}
