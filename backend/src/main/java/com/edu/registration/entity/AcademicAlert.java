package com.edu.registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Thực thể Cảnh báo học tập sớm (Early Warning Academic Alert - QLSV-97 / TASK-OP-10).
 *
 * <p>Được sinh ra theo các quy tắc minh bạch, giải thích được, dựa trên dữ liệu thật.
 * KHÔNG dùng mô hình hộp đen.
 * KHÔNG tự ý thay đổi trạng thái học vụ sinh viên, KHÔNG khóa tài khoản và KHÔNG sửa dữ liệu điểm số/đăng ký.</p>
 */
@Entity
@Table(
        name = "canh_bao_hoc_tap",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_canh_bao_sv_luat_ngucanh",
                columnNames = "khoa_chong_trung"
        ),
        indexes = {
                @Index(name = "idx_cbht_sinh_vien", columnList = "sinh_vien_id"),
                @Index(name = "idx_cbht_loai_luat", columnList = "loai_luat"),
                @Index(name = "idx_cbht_trang_thai", columnList = "trang_thai"),
                @Index(name = "idx_cbht_active", columnList = "active"),
                @Index(name = "idx_cbht_lop_hoc_phan", columnList = "lop_hoc_phan_id")
        }
)
public class AcademicAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private Student sinhVien;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_luat", nullable = false, length = 40)
    private AcademicAlertRuleType loaiLuat;

    @Column(name = "ma_luat", nullable = false, length = 60)
    private String maLuat;

    @Column(name = "ten_luat", nullable = false, length = 150)
    private String tenLuat;

    @Enumerated(EnumType.STRING)
    @Column(name = "muc_do", nullable = false, length = 30)
    private AcademicAlertSeverity mucDo;

    @Column(name = "ly_do", nullable = false, length = 1000)
    private String lyDo;

    @Column(name = "cong_thuc_giai_thich", nullable = false, length = 2000)
    private String congThucGiaiThich;

    @Column(name = "du_lieu_dan_chung", nullable = false, length = 4000)
    private String duLieuDanChung;

    @Column(name = "ngu_canh", nullable = false, length = 150)
    private String nguCanh;

    @Column(name = "khoa_chong_trung", nullable = false, length = 250)
    private String khoaChongTrung;

    @Column(name = "lop_hoc_phan_id")
    private Long lopHocPhanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private AcademicAlertStatus trangThai = AcademicAlertStatus.DANG_CANH_BAO;

    @Column(name = "cho_phep_sinh_vien_xem", nullable = false)
    private boolean choPhepSinhVienXem = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    @Column(name = "cap_nhat_luc", nullable = false)
    private LocalDateTime capNhatLuc = LocalDateTime.now();

    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;

    @Column(name = "nguoi_xu_ly", length = 80)
    private String nguoiXuLy;

    @Column(name = "ghi_chu_xu_ly", length = 1000)
    private String ghiChuXuLy;

    @Column(name = "nguyen_nhan_giai_toa", length = 1000)
    private String nguyenNhanGiaiToa;

    public AcademicAlert() {
    }

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

    public AcademicAlertRuleType getLoaiLuat() {
        return loaiLuat;
    }

    public void setLoaiLuat(AcademicAlertRuleType loaiLuat) {
        this.loaiLuat = loaiLuat;
    }

    public String getMaLuat() {
        return maLuat;
    }

    public void setMaLuat(String maLuat) {
        this.maLuat = maLuat;
    }

    public String getTenLuat() {
        return tenLuat;
    }

    public void setTenLuat(String tenLuat) {
        this.tenLuat = tenLuat;
    }

    public AcademicAlertSeverity getMucDo() {
        return mucDo;
    }

    public void setMucDo(AcademicAlertSeverity mucDo) {
        this.mucDo = mucDo;
    }

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String lyDo) {
        this.lyDo = lyDo;
    }

    public String getCongThucGiaiThich() {
        return congThucGiaiThich;
    }

    public void setCongThucGiaiThich(String congThucGiaiThich) {
        this.congThucGiaiThich = congThucGiaiThich;
    }

    public String getDuLieuDanChung() {
        return duLieuDanChung;
    }

    public void setDuLieuDanChung(String duLieuDanChung) {
        this.duLieuDanChung = duLieuDanChung;
    }

    public String getNguCanh() {
        return nguCanh;
    }

    public void setNguCanh(String nguCanh) {
        this.nguCanh = nguCanh;
    }

    public String getKhoaChongTrung() {
        return khoaChongTrung;
    }

    public void setKhoaChongTrung(String khoaChongTrung) {
        this.khoaChongTrung = khoaChongTrung;
    }

    public Long getLopHocPhanId() {
        return lopHocPhanId;
    }

    public void setLopHocPhanId(Long lopHocPhanId) {
        this.lopHocPhanId = lopHocPhanId;
    }

    public AcademicAlertStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(AcademicAlertStatus trangThai) {
        this.trangThai = trangThai;
        this.active = trangThai != null && trangThai.isActive();
    }

    public boolean isChoPhepSinhVienXem() {
        return choPhepSinhVienXem;
    }

    public void setChoPhepSinhVienXem(boolean choPhepSinhVienXem) {
        this.choPhepSinhVienXem = choPhepSinhVienXem;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getCapNhatLuc() {
        return capNhatLuc;
    }

    public void setCapNhatLuc(LocalDateTime capNhatLuc) {
        this.capNhatLuc = capNhatLuc;
    }

    public LocalDateTime getNgayXuLy() {
        return ngayXuLy;
    }

    public void setNgayXuLy(LocalDateTime ngayXuLy) {
        this.ngayXuLy = ngayXuLy;
    }

    public String getNguoiXuLy() {
        return nguoiXuLy;
    }

    public void setNguoiXuLy(String nguoiXuLy) {
        this.nguoiXuLy = nguoiXuLy;
    }

    public String getGhiChuXuLy() {
        return ghiChuXuLy;
    }

    public void setGhiChuXuLy(String ghiChuXuLy) {
        this.ghiChuXuLy = ghiChuXuLy;
    }

    public String getNguyenNhanGiaiToa() {
        return nguyenNhanGiaiToa;
    }

    public void setNguyenNhanGiaiToa(String nguyenNhanGiaiToa) {
        this.nguyenNhanGiaiToa = nguyenNhanGiaiToa;
    }
}
