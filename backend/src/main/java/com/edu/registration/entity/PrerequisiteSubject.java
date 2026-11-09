package com.edu.registration.entity;

import com.edu.registration.service.AcademicRules;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mon_tien_quyet", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"mon_hoc_id", "mon_tien_quyet_id", "loai_quan_he"})
})
public class PrerequisiteSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_id", nullable = false)
    private Subject monHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_tien_quyet_id", nullable = false)
    private Subject monTienQuyet;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_quan_he", length = 30)
    private SubjectRelationType loaiQuanHe = SubjectRelationType.TIEN_QUYET;

    @Column(name = "dieu_kien_dat")
    private Double dieuKienDat = AcademicRules.DIEM_DAT;

    @Column(name = "yeu_cau_dat")
    private Boolean yeuCauDat = Boolean.TRUE;

    @Column(name = "ma_nhom_tu_chon", length = 60)
    private String maNhomTuChon;

    @Column(name = "tin_chi_tu_chon_yeu_cau")
    private Integer tinChiTuChonYeuCau;

    @Column(name = "khoa_nhap_hoc", length = 50)
    private String khoaNhapHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_hieu_luc_tu_id")
    private Semester hocKyHieuLucTu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_hieu_luc_den_id")
    private Semester hocKyHieuLucDen;

    @Column(name = "hieu_luc_tu")
    private LocalDate hieuLucTu;

    @Column(name = "hieu_luc_den")
    private LocalDate hieuLucDen;

    @Column(name = "dang_hieu_luc")
    private Boolean dangHieuLuc = Boolean.TRUE;

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

    public PrerequisiteSubject() {
    }

    public PrerequisiteSubject(Subject monHoc, Subject monTienQuyet) {
        this.monHoc = monHoc;
        this.monTienQuyet = monTienQuyet;
        this.loaiQuanHe = SubjectRelationType.TIEN_QUYET;
        this.dieuKienDat = AcademicRules.DIEM_DAT;
        this.yeuCauDat = Boolean.TRUE;
        this.dangHieuLuc = Boolean.TRUE;
    }

    public PrerequisiteSubject(Subject monHoc, Subject monTienQuyet, SubjectRelationType loaiQuanHe) {
        this(monHoc, monTienQuyet);
        this.loaiQuanHe = loaiQuanHe != null ? loaiQuanHe : SubjectRelationType.TIEN_QUYET;
    }

    public SubjectRelationType getLoaiQuanHeHieuLuc() {
        return loaiQuanHe != null ? loaiQuanHe : SubjectRelationType.TIEN_QUYET;
    }

    public boolean laQuanHeTienQuyet() {
        return getLoaiQuanHeHieuLuc() == SubjectRelationType.TIEN_QUYET;
    }

    public double getDieuKienDatHieuLuc() {
        return dieuKienDat != null ? dieuKienDat : AcademicRules.DIEM_DAT;
    }

    public boolean isYeuCauDatHieuLuc() {
        return yeuCauDat == null || yeuCauDat;
    }

    public boolean isDangHieuLucHieuLuc() {
        return dangHieuLuc == null || dangHieuLuc;
    }

    /**
     * Kiểm tra quan hệ môn có đang trong đợt/thời gian/khóa hiệu lực đối với học kỳ và khóa của sinh viên hay không.
     * Nếu trường cấu hình để trống (null), coi như áp dụng không giới hạn theo chiều đó.
     */
    public boolean laHieuLucCho(Semester hocKy, String khoaSinhVien, LocalDate ngayKiemTra) {
        if (!isDangHieuLucHieuLuc()) {
            return false;
        }
        LocalDate mocNgay = ngayKiemTra != null ? ngayKiemTra : LocalDate.now();
        if (hieuLucTu != null && mocNgay.isBefore(hieuLucTu)) {
            return false;
        }
        if (hieuLucDen != null && mocNgay.isAfter(hieuLucDen)) {
            return false;
        }
        if (hocKy != null && hocKy.getNgayBatDau() != null) {
            if (hocKyHieuLucTu != null && hocKyHieuLucTu.getNgayBatDau() != null
                    && hocKy.getNgayBatDau().isBefore(hocKyHieuLucTu.getNgayBatDau())) {
                return false;
            }
            if (hocKyHieuLucDen != null && hocKyHieuLucDen.getNgayBatDau() != null
                    && hocKy.getNgayBatDau().isAfter(hocKyHieuLucDen.getNgayBatDau())) {
                return false;
            }
        }
        if (khoaNhapHoc != null && !khoaNhapHoc.isBlank()) {
            if (khoaSinhVien == null || khoaSinhVien.isBlank()
                    || !khoaNhapHoc.trim().equalsIgnoreCase(khoaSinhVien.trim())) {
                return false;
            }
        }
        return true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Subject getMonHoc() {
        return monHoc;
    }

    public void setMonHoc(Subject monHoc) {
        this.monHoc = monHoc;
    }

    public Subject getMonTienQuyet() {
        return monTienQuyet;
    }

    public void setMonTienQuyet(Subject monTienQuyet) {
        this.monTienQuyet = monTienQuyet;
    }

    public SubjectRelationType getLoaiQuanHe() {
        return getLoaiQuanHeHieuLuc();
    }

    public void setLoaiQuanHe(SubjectRelationType loaiQuanHe) {
        this.loaiQuanHe = loaiQuanHe != null ? loaiQuanHe : SubjectRelationType.TIEN_QUYET;
    }

    public Double getDieuKienDat() {
        return dieuKienDat;
    }

    public void setDieuKienDat(Double dieuKienDat) {
        this.dieuKienDat = dieuKienDat;
    }

    public Boolean getYeuCauDat() {
        return yeuCauDat;
    }

    public void setYeuCauDat(Boolean yeuCauDat) {
        this.yeuCauDat = yeuCauDat;
    }

    public String getMaNhomTuChon() {
        return maNhomTuChon;
    }

    public void setMaNhomTuChon(String maNhomTuChon) {
        this.maNhomTuChon = maNhomTuChon;
    }

    public Integer getTinChiTuChonYeuCau() {
        return tinChiTuChonYeuCau;
    }

    public void setTinChiTuChonYeuCau(Integer tinChiTuChonYeuCau) {
        this.tinChiTuChonYeuCau = tinChiTuChonYeuCau;
    }

    public String getKhoaNhapHoc() {
        return khoaNhapHoc;
    }

    public void setKhoaNhapHoc(String khoaNhapHoc) {
        this.khoaNhapHoc = khoaNhapHoc;
    }

    public Semester getHocKyHieuLucTu() {
        return hocKyHieuLucTu;
    }

    public void setHocKyHieuLucTu(Semester hocKyHieuLucTu) {
        this.hocKyHieuLucTu = hocKyHieuLucTu;
    }

    public Semester getHocKyHieuLucDen() {
        return hocKyHieuLucDen;
    }

    public void setHocKyHieuLucDen(Semester hocKyHieuLucDen) {
        this.hocKyHieuLucDen = hocKyHieuLucDen;
    }

    public LocalDate getHieuLucTu() {
        return hieuLucTu;
    }

    public void setHieuLucTu(LocalDate hieuLucTu) {
        this.hieuLucTu = hieuLucTu;
    }

    public LocalDate getHieuLucDen() {
        return hieuLucDen;
    }

    public void setHieuLucDen(LocalDate hieuLucDen) {
        this.hieuLucDen = hieuLucDen;
    }

    public Boolean getDangHieuLuc() {
        return isDangHieuLucHieuLuc();
    }

    public void setDangHieuLuc(Boolean dangHieuLuc) {
        this.dangHieuLuc = dangHieuLuc != null ? dangHieuLuc : Boolean.TRUE;
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
