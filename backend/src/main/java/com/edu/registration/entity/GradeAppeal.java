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
@Table(name = "ho_so_phuc_khao", indexes = {
        @Index(name = "idx_phuc_khao_sinh_vien", columnList = "sinh_vien_id,created_at"),
        @Index(name = "idx_phuc_khao_lop_hoc_phan", columnList = "lop_hoc_phan_id"),
        @Index(name = "idx_phuc_khao_trang_thai", columnList = "trang_thai,created_at"),
        @Index(name = "idx_phuc_khao_ma_ho_so", columnList = "ma_ho_so", unique = true)
})
public class GradeAppeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ho_so", nullable = false, unique = true, length = 32, updatable = false)
    private String maHoSo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinh_vien_id", nullable = false, updatable = false)
    private Student sinhVien;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false, updatable = false)
    private CourseClass lopHocPhan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mon_hoc_id", nullable = false, updatable = false)
    private Subject monHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dang_ky_id", nullable = false, updatable = false)
    private CourseRegistration dangKy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diem_id", nullable = false, updatable = false)
    private Grade diem;

    // Snapshot điểm gốc bất biến tại thời điểm nộp đơn phúc khảo
    @Column(name = "diem_qua_trinh_goc", updatable = false)
    private Double diemQuaTrinhGoc;

    @Column(name = "diem_cuoi_ky_goc", updatable = false)
    private Double diemCuoiKyGoc;

    @Column(name = "diem_tong_ket_goc", updatable = false)
    private Double diemTongKetGoc;

    // Thành phần điểm yêu cầu phúc khảo: DIEM_QUA_TRINH, DIEM_CUOI_KY, CA_HAI
    @Column(name = "loai_diem", nullable = false, length = 32, updatable = false)
    private String loaiDiem = "DIEM_CUOI_KY";

    @Column(name = "diem_mong_muon", updatable = false)
    private Double diemMongMuon;

    @Column(name = "ly_do", nullable = false, length = 2000, updatable = false)
    private String lyDo;

    // Hạn nộp phúc khảo tính theo cấu hình tập trung
    @Column(name = "han_nop", updatable = false)
    private LocalDateTime hanNop;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 32)
    private GradeAppealStatus trangThai = GradeAppealStatus.MOI_TAO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_xu_ly_user_id")
    private User nguoiXuLy;

    @Column(name = "thoi_diem_xu_ly")
    private LocalDateTime thoiDiemXuLy;

    @Column(name = "ket_qua_xu_ly", length = 2000)
    private String ketQuaXuLy;

    // Điểm mới sau khi xử lý chấp thuận điều chỉnh
    @Column(name = "diem_qua_trinh_moi")
    private Double diemQuaTrinhMoi;

    @Column(name = "diem_cuoi_ky_moi")
    private Double diemCuoiKyMoi;

    @Column(name = "diem_tong_ket_moi")
    private Double diemTongKetMoi;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaHoSo() {
        return maHoSo;
    }

    public void setMaHoSo(String maHoSo) {
        this.maHoSo = maHoSo;
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

    public Subject getMonHoc() {
        return monHoc;
    }

    public void setMonHoc(Subject monHoc) {
        this.monHoc = monHoc;
    }

    public CourseRegistration getDangKy() {
        return dangKy;
    }

    public void setDangKy(CourseRegistration dangKy) {
        this.dangKy = dangKy;
    }

    public Grade getDiem() {
        return diem;
    }

    public void setDiem(Grade diem) {
        this.diem = diem;
    }

    public Double getDiemQuaTrinhGoc() {
        return diemQuaTrinhGoc;
    }

    public void setDiemQuaTrinhGoc(Double diemQuaTrinhGoc) {
        this.diemQuaTrinhGoc = diemQuaTrinhGoc;
    }

    public Double getDiemCuoiKyGoc() {
        return diemCuoiKyGoc;
    }

    public void setDiemCuoiKyGoc(Double diemCuoiKyGoc) {
        this.diemCuoiKyGoc = diemCuoiKyGoc;
    }

    public Double getDiemTongKetGoc() {
        return diemTongKetGoc;
    }

    public void setDiemTongKetGoc(Double diemTongKetGoc) {
        this.diemTongKetGoc = diemTongKetGoc;
    }

    public String getLoaiDiem() {
        return loaiDiem;
    }

    public void setLoaiDiem(String loaiDiem) {
        this.loaiDiem = loaiDiem;
    }

    public Double getDiemMongMuon() {
        return diemMongMuon;
    }

    public void setDiemMongMuon(Double diemMongMuon) {
        this.diemMongMuon = diemMongMuon;
    }

    public String getLyDo() {
        return lyDo;
    }

    public void setLyDo(String lyDo) {
        this.lyDo = lyDo;
    }

    public LocalDateTime getHanNop() {
        return hanNop;
    }

    public void setHanNop(LocalDateTime hanNop) {
        this.hanNop = hanNop;
    }

    public GradeAppealStatus getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(GradeAppealStatus trangThai) {
        this.trangThai = trangThai;
    }

    public User getNguoiXuLy() {
        return nguoiXuLy;
    }

    public void setNguoiXuLy(User nguoiXuLy) {
        this.nguoiXuLy = nguoiXuLy;
    }

    public LocalDateTime getThoiDiemXuLy() {
        return thoiDiemXuLy;
    }

    public void setThoiDiemXuLy(LocalDateTime thoiDiemXuLy) {
        this.thoiDiemXuLy = thoiDiemXuLy;
    }

    public String getKetQuaXuLy() {
        return ketQuaXuLy;
    }

    public void setKetQuaXuLy(String ketQuaXuLy) {
        this.ketQuaXuLy = ketQuaXuLy;
    }

    public Double getDiemQuaTrinhMoi() {
        return diemQuaTrinhMoi;
    }

    public void setDiemQuaTrinhMoi(Double diemQuaTrinhMoi) {
        this.diemQuaTrinhMoi = diemQuaTrinhMoi;
    }

    public Double getDiemCuoiKyMoi() {
        return diemCuoiKyMoi;
    }

    public void setDiemCuoiKyMoi(Double diemCuoiKyMoi) {
        this.diemCuoiKyMoi = diemCuoiKyMoi;
    }

    public Double getDiemTongKetMoi() {
        return diemTongKetMoi;
    }

    public void setDiemTongKetMoi(Double diemTongKetMoi) {
        this.diemTongKetMoi = diemTongKetMoi;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
